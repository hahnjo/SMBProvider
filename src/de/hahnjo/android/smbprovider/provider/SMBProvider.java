package de.hahnjo.android.smbprovider.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.accounts.Account;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.util.Log;
import de.hahnjo.android.smbprovider.BuildConfig;
import de.hahnjo.android.smbprovider.SMBConnection;
import de.hahnjo.android.smbprovider.account.SMBAccountAuthenticator;
import de.hahnjo.android.smbprovider.provider.cursor.DocumentCursor;
import de.hahnjo.android.smbprovider.provider.cursor.ExtraLoadingCursor;
import de.hahnjo.android.smbprovider.provider.cursor.RootCursor;
import de.hahnjo.android.smbprovider.provider.cursor.RootDocumentCursor;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import static android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED;
import static android.provider.DocumentsContract.Document.COLUMN_SIZE;

/**
 * The {@link DocumentsProvider} that gives the Android system access to files on SMB shares.
 */
public class SMBProvider extends DocumentsProvider {

	private static final String TAG = SMBProvider.class.getSimpleName();

	private static final String AUTHORITY = "de.hahnjo.android.smbprovider";

	private DocumentDatabase database;

	private volatile ChildQueryResult lastChildQueryResult;

	@Override
	public boolean onCreate() {
		if (BuildConfig.DEBUG) Log.d(TAG, "onCreate");

		database = new DocumentDatabase(getContext());
		return true;
	}

	@Override
	public Cursor queryRoots(String[] projection) throws FileNotFoundException {
		if (BuildConfig.DEBUG) Log.d(TAG, "queryRoots");

		return new RootCursor(getContext(), projection);
	}

	@Override
	public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
		if (BuildConfig.DEBUG) Log.d(TAG, "queryDocument: documentId=" + documentId);

		if (DocumentIdUtils.isRoot(documentId)) {
			if (BuildConfig.DEBUG) Log.d(TAG, "This is a root, so let's return a RootDocumentCursor");
			return new RootDocumentCursor(projection, documentId);
		}
		if (BuildConfig.DEBUG) Log.d(TAG, "This seems to be a document...");

		final String[] projectionOriginal = projection;
		final int projectionOriginalLength;
		int cLastModified = -1; // column index, yet unknown
		boolean wantsLastModifiedCol = false; // till proven otherwise
		boolean wantsVolatileCol = false; // i.e. one whose value might change and become stale
		if (projection == null) {
			projectionOriginalLength = -1;
			wantsLastModifiedCol = true;
			wantsVolatileCol = true;
		} else {
			projectionOriginalLength = projection.length;
			for (int c = 0; c < projectionOriginalLength; ++c) {
				final String columnName = projection[c];
				if (COLUMN_LAST_MODIFIED.equals(columnName)) {
					cLastModified = c;
					wantsLastModifiedCol = true;
					wantsVolatileCol = true;
					break;
				}
				if (!wantsVolatileCol && COLUMN_SIZE.equals(columnName)) {
					wantsVolatileCol = true;
				}
			}
		}

		final Document docLastListed = fromLastChildQuery(documentId);
		if (docLastListed != null) {
			if (BuildConfig.DEBUG) Log.d(TAG, "The document " + docLastListed.name + " was in the last directory that was queried");

			if (wantsVolatileCol) {
				if (!wantsLastModifiedCol) { // then add it regardless because it's needed below
					projection = Arrays.copyOf(projection, projectionOriginalLength + 1);
					cLastModified = projectionOriginalLength;
					projection[cLastModified] = COLUMN_LAST_MODIFIED;
				}
			}
			else { // docLastListed cannot be stale under this projection, so just return it:
				return new DocumentCursor(projectionOriginal, docLastListed);
		    }
		}

		Cursor cursor = database.getReadableDatabase().query(DocumentDatabase.TABLE_NAME, projection,
			DocumentDatabase.Columns.DOCUMENT_ID + "=?", new String[] { documentId }, null, null, null);
		if (cursor.getCount() == 1) {
			if (BuildConfig.DEBUG) Log.d(TAG, "Information about the document " + DocumentIdUtils.getName(documentId) + " was in the database");

			if (docLastListed == null) {
				return cursor; // database cursor is all that is known, so just return it
			}

			if (cLastModified == -1) { // happens when projectionOriginal is null
				cLastModified = cursor.getColumnIndexOrThrow(COLUMN_LAST_MODIFIED);
			}
			cursor.moveToFirst();
			if (cursor.getLong(cLastModified) > docLastListed.lastModified) {
				return cursor; // database cursor is fresher than docLastListed, so return it
			}
		}

		cursor.close();
		if (docLastListed != null) {
			// it's either all that's known, or no staler than database cursor, so return it:
			return new DocumentCursor(projectionOriginal, docLastListed);
		}

		throw new FileNotFoundException();
	}

	@Override
	public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
		if (BuildConfig.DEBUG) Log.d(TAG, "queryChildDocuments: parentDocumentId=" + parentDocumentId);

		final ChildQueryResult last = lastChildQueryResult; // thread-safe reference
		final Cursor cursor;
		final boolean isRefreshNeeded;
		if (last != null && parentDocumentId.equals(last.parent.documentId)) {
			final Bundle extras;
			if (System.nanoTime() - last.nanoTime < 2_000_000_000L) {
				// The last result was fetched from the network as "extra loading" data
				// less than a few seconds ago, and clients were notified at that time.
				// Likely this is just a re-query to pick up the fresh data.  In any case,
				// the data is fresh enough to assume as current.
				isRefreshNeeded = false;
				extras = Bundle.EMPTY;
			} else {
				// The last result is old and possibly stale.  It needs to be refreshed
				// and the client advised to expect notification.
				isRefreshNeeded = true;
				extras = ExtraLoadingCursor.EXTRA_LOADING_BUNDLE;
			}
			cursor = new DocumentCursor(projection, last.childArray, extras);
		} else { // there is no cached child result *at all* for this parent
			cursor = new ExtraLoadingCursor();
			isRefreshNeeded = true;
		}
		if (isRefreshNeeded) {
			cursor.setNotificationUri(getContext().getContentResolver(), DocumentsContract.buildDocumentUri(AUTHORITY, parentDocumentId));
			new DirectoryListFetcherThread(parentDocumentId).start();
		}
		return cursor;
	}

	@Override
	public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
		if (BuildConfig.DEBUG) Log.d(TAG, "openDocument: documentId=" + documentId);

		File cacheFile = getFileForDocument(documentId);
		boolean exists = cacheFile.exists();
		if (exists) {
			final Document doc = fromLastChildQuery(documentId);
			if (doc != null && doc.lastModified - cacheFile.lastModified() > 999L) {
				// allowing 999 ms for difference in clock granularity between file systems
				if (cacheFile.delete()) exists = false; // will download a fresh one
				else Log.w(TAG, "Unable to overwrite stale file that was previously downloaded");
			}
		}
		if (!exists) {
			if (BuildConfig.DEBUG) Log.d(TAG, "We must download the document...");
			try {
				if (cacheFile.getParentFile().mkdirs() && !cacheFile.createNewFile()) {
					throw new FileNotFoundException("File could not be created!");
				}

				String accountName = DocumentIdUtils.getAccountName(documentId);
				String path = DocumentIdUtils.getPath(documentId);

				SMBConnection connection = getConnection(accountName);
				SmbFile remote = connection.openPath(path);
				// We have to create the document before we download it because otherwise, at least lastModified won't be correct. Don't ask why!
				Document document = new Document(remote, documentId, false);

				boolean success = downloadFile(remote, cacheFile, signal);
				if (!success) {
					cacheFile.delete();
					throw new FileNotFoundException();
				}

				if (!cacheFile.setLastModified(remote.lastModified())) {
					Log.w(TAG, "Unable to synchronize modification time of downloaded file");
				}

				database.getWritableDatabase().insert(DocumentDatabase.TABLE_NAME, null, document.toContentValues());

			} catch (IOException e) {
				throw new FileNotFoundException( e.toString() );
			}
		}
		return ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);
	}

	@Override
	public boolean isChildDocument(String parentDocumentId, String documentId) {
		// adds support for directory selection (API 21) via ACTION_OPEN_DOCUMENT_TREE
		return documentId.startsWith(parentDocumentId) // starts with parentDocumentId
		  && documentId.length() != parentDocumentId.length(); // but does not equal it
	}

	/**
	 * Downloads the remote file and writes its content to the local file. Also takes care whether the signal is cancelled.
	 *
	 * @return true, if the file was successfully downloaded
	 */
	private boolean downloadFile(SmbFile remote, File local, CancellationSignal signal) {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = remote.getInputStream();
			output = new FileOutputStream(local);

			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);

				if (signal != null && signal.isCanceled()) {
					return false;
				}
			}

			return true;

		} catch (Throwable t) {
			Log.e(TAG, "Error occurred while downloading the file", t);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					Log.w(TAG, e);
				}
			}

			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					Log.w(TAG, e);
				}
			}
		}

		return false;
	}

	/**
	 * Fetches the list of files and directories under this documentId. Caches this result and then notifies the {@link ExtraLoadingCursor} via the
	 * {@link ContentResolver} that it shall request the new data.
	 */
	private void fetchDirectoryList(String documentId) {
		String accountName = DocumentIdUtils.getAccountName(documentId);
		String path = DocumentIdUtils.getPath(documentId);
		SMBConnection connection = getConnection(accountName);

		try {
			SmbFile directory = connection.openPath(path);
			SmbFile[] files = directory.listFiles();

			Document[] documents = new Document[files.length];
			for (int i = 0; i < files.length; i++) {
				SmbFile file = files[i];
				documents[i] = new Document(file, documentId);
			}

			lastChildQueryResult = new ChildQueryResult(
			  new Document(directory,documentId,/*appendName*/false), documents );

			getContext().getContentResolver().notifyChange(DocumentsContract.buildDocumentUri(AUTHORITY, documentId), null);
		} catch (SmbException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Searches lastChildQueryResult and returns either the specified document, or null.
	 */
	private Document fromLastChildQuery(final String id) {

		final ChildQueryResult last = lastChildQueryResult; // take thread-safe snapshot of whole result
		if (last == null) {
			return null;
		}

		final Document parent = last.parent;
		final String parentId = parent.documentId;
		if (!id.startsWith(parentId)) {
			return null;
		}

		if (id.length() == parentId.length()) {
			return parent;
		}

		for (final Document child: last.childArray) {
			if (id.equals(child.documentId)) {
				return child;
			}
		}

		Log.w(TAG, "Document missing in lastChildQueryResult although directory ID is same");
		return null;
	}

	private SMBConnection getConnection(String accountName) {
		Account account = SMBAccountAuthenticator.getAccount(getContext(), accountName);
		return new SMBConnection(getContext(), account);
	}

	private File getFileForDocument(String documentId) {
		File cacheDir = getContext().getCacheDir();
		return new File(cacheDir.getAbsolutePath() + "/" + documentId);
	}

	private class DirectoryListFetcherThread extends Thread {

		private final String documentId;

		public DirectoryListFetcherThread(String documentId) {
			this.documentId = documentId;
		}

		@Override
		public void run() {
			fetchDirectoryList(documentId);
		}
	}

	/**
	 * Query result data, lumped together and immutable for thread safety.
	 */
	private static final class ChildQueryResult {

		ChildQueryResult(Document _parent, Document[] _childArray) {
			parent = _parent;
			childArray = _childArray;
		}

		final Document parent;
		final Document[] childArray;
		final long nanoTime = System.nanoTime();
	}

}
