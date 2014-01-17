package de.hahnjo.android.smbprovider.provider;

import android.content.ContentValues;
import android.provider.DocumentsContract;
import android.webkit.MimeTypeMap;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * A class that contains information about one single document. This may either be a directory or a file.
 */
public class Document {

	public final String name;
	public final String documentId;
	public final int flags;
	public final long lastModified;
	public final String mimeType;
	public final long size;

	/**
	 * Creates a new document that holds information about this {@link SmbFile}. If appendName is true, its name will be appended to the documentId.
	 */
	public Document(SmbFile file, String documentId, boolean appendName) throws SmbException {
		String name = file.getName();

		if (appendName) {
			this.documentId = documentId + name;
		} else {
			this.documentId = documentId;
		}

		// Do this after the documentId, so the documentId has the trailing slash
		if (file.isDirectory()) {
			// Remove trailing slash
			name = name.substring(0, name.length() - 1);
		}
		this.name = name;

		flags = 0;
		lastModified = file.lastModified();

		if (file.isDirectory()) {
			mimeType = DocumentsContract.Document.MIME_TYPE_DIR;
		} else {
			mimeType = getMimeType();
		}

		size = file.length();
	}

	/**
	 * Creates a new document that holds information about this {@link SmbFile}. Its name will be appended to its parentDocumentId.
	 */
	public Document(SmbFile file, String parentDocumentId) throws SmbException {
		this(file, parentDocumentId, true);
	}

	/**
	 * @return the MIME-Type for this document (determined by its extension)
	 */
	private String getMimeType() {
		String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
	}

	/**
	 * @return {@link ContentValues} that may be inserted into a database
	 */
	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();

		values.put(DocumentDatabase.Columns.DISPLAY_NAME, name);
		values.put(DocumentDatabase.Columns.DOCUMENT_ID, documentId);
		values.put(DocumentDatabase.Columns.FLAGS, flags);
		values.put(DocumentDatabase.Columns.LAST_MODIFIED, lastModified);
		values.put(DocumentDatabase.Columns.MIME_TYPE, mimeType);
		values.put(DocumentDatabase.Columns.SIZE, size);

		return values;
	}

}
