package de.hahnjo.android.smbprovider.provider.cursor;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.DocumentsContract;
import de.hahnjo.android.smbprovider.provider.Document;

/**
 * A {@link Cursor} that contains information about documents as specified by {@link DocumentsContract.Document}.
 */
public class DocumentCursor extends MatrixCursor {

	private static final String[] DEFAULT_PROJECTION = new String[] {
		DocumentsContract.Document.COLUMN_DISPLAY_NAME,
		DocumentsContract.Document.COLUMN_DOCUMENT_ID,
		DocumentsContract.Document.COLUMN_FLAGS,
		DocumentsContract.Document.COLUMN_LAST_MODIFIED,
		DocumentsContract.Document.COLUMN_MIME_TYPE,
		DocumentsContract.Document.COLUMN_SIZE
	};

	/**
	 * Creates an empty cursor.
	 */
	public DocumentCursor(String[] projection) {
		super(projection == null ? DEFAULT_PROJECTION : projection);
	}

	/**
	 * Creates a cursor that is filled with one {@link Document}.
	 */
	public DocumentCursor(String[] projection, Document document) {
		this(projection);

		addRow(document);
	}

	/**
	 * Creates a cursor that is filled with multiple {@link Document}s.
	 */
	public DocumentCursor(String[] projection, Document[] documents) {
		this(projection);

		for (Document document : documents) {
			addRow(document);
		}
	}

	/**
	 * Adds a new row for this {@link Document}.
	 */
	protected void addRow(Document document) {
		newRow().add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, document.name)
			.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, document.documentId)
			.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
			.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, document.lastModified)
			.add(DocumentsContract.Document.COLUMN_MIME_TYPE, document.mimeType)
			.add(DocumentsContract.Document.COLUMN_SIZE, document.size);
	}

}
