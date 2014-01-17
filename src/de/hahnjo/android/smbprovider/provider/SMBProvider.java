package de.hahnjo.android.smbprovider.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import android.accounts.Account;
import android.content.ContentResolver;
import android.database.Cursor;
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

/**
 * The {@link DocumentsProvider} that gives the Android system access to files on SMB shares.
 */
public class SMBProvider extends DocumentsProvider {

	private static final String TAG = SMBProvider.class.getSimpleName();

	private static final String AUTHORITY = "de.hahnjo.android.smbprovider";

	private Map<String, Cursor> cursorCache = new HashMap<String, Cursor>();
	private DocumentDatabase database;

	private String lastDirectoryDocumentId;
	private Document[] lastDirectoryDocumentList;

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
		} else if (DocumentIdUtils.isDirectory(documentId)) {
			Log.w(TAG, "What does this app want to know about a DIRECTORY?!?");
			return null;
		}
		if (BuildConfig.DEBUG) Log.d(TAG, "This seems to be a document...");

		Cursor cursor = database.getReadableDatabase().query(DocumentDatabase.TABLE_NAME, projection,
			DocumentDatabase.Columns.DOCUMENT_ID + "=?", new String[] { documentId }, null, null, null);
		if (cursor.getCount() == 1) {
			if (BuildConfig.DEBUG) Log.d(TAG, "Information about the document " + DocumentIdUtils.getName(documentId) + " was in the database");

			return cursor;
		}

		if (lastDirectoryDocumentId != null && lastDirectoryDocumentList != null && documentId.startsWith(lastDirectoryDocumentId)) {
			String name = DocumentIdUtils.getName(documentId);

			for (Document document : lastDirectoryDocumentList) {
				if (name.equals(document.name)) {
					if (BuildConfig.DEBUG) Log.d(TAG, "The document " + name + " was in the last directory that was queried");

					try {
						return new DocumentCursor(projection, document);
					} catch (SmbException e) {
						Log.e(TAG, "Error occurred while retrieving information about a document", e);
					}
				}
			}

			Log.w(TAG, "File was not found in list although the documentId of the directory is the same");
		}

		return null;
	}

	@Override
	public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
		if (BuildConfig.DEBUG) Log.d(TAG, "queryChildDocuments: parentDocumentId=" + parentDocumentId);

		Cursor cursor = cursorCache.remove(parentDocumentId);
		if (cursor == null) {
			cursor = new ExtraLoadingCursor();
			cursor.setNotificationUri(getContext().getContentResolver(), DocumentsContract.buildDocumentUri(AUTHORITY, parentDocumentId));

			new DirectoryListFetcherThread(parentDocumentId, projection).start();
		}

		return cursor;
	}

	@Override
	public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal) throws FileNotFoundException {
		if (BuildConfig.DEBUG) Log.d(TAG, "openDocument: documentId=" + documentId);

		File cacheFile = getFileForDocument(documentId);
		if (!cacheFile.exists()) {
			if (BuildConfig.DEBUG) Log.d(TAG, "We must download the document...");
			try {
				if (cacheFile.getParentFile().mkdirs() && !cacheFile.createNewFile()) {
					Log.e(TAG, "File could not be created!");
					return null;
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
					return null;
				}

				database.getWritableDatabase().insert(DocumentDatabase.TABLE_NAME, null, document.toContentValues());

			} catch (IOException e) {
				return null;
			}
		}
		return ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY);
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
	private void fetchDirectoryList(String documentId, String[] projection) {
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

			Cursor cursor = new DocumentCursor(projection, documents);

			lastDirectoryDocumentId = documentId;
			lastDirectoryDocumentList = documents;

			cursorCache.put(documentId, cursor);
			getContext().getContentResolver().notifyChange(DocumentsContract.buildDocumentUri(AUTHORITY, documentId), null);
		} catch (SmbException e) {
			e.printStackTrace();
		}
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
		private final String[] projection;

		public DirectoryListFetcherThread(String documentId, String[] projection) {
			this.documentId = documentId;
			this.projection = projection;
		}

		@Override
		public void run() {
			fetchDirectoryList(documentId, projection);
		}
	}

}
