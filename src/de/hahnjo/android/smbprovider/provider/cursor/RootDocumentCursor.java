package de.hahnjo.android.smbprovider.provider.cursor;

import android.database.Cursor;
import android.provider.DocumentsContract;

/**
 * A {@link Cursor} that contains information about one single root as specified by {@link DocumentsContract.Document}.
 */
public class RootDocumentCursor extends DocumentCursor {

	/**
	 * Creates a new cursor that is filled with information about the root that is identified by this documentId.
	 */
	public RootDocumentCursor(String[] projection, String documentId) {
		super(projection);

		String name = documentId.substring(0, documentId.length() - 1);
		newRow().add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name)
			.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
			.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
			.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, null)
			.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
			.add(DocumentsContract.Document.COLUMN_SIZE, null);
	}

}
