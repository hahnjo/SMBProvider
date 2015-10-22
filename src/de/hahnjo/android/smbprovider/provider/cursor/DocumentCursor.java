package de.hahnjo.android.smbprovider.provider.cursor;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
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

	private final Bundle extras;

	/**
	 * Creates an empty cursor.
	 */
	public DocumentCursor(String[] projection) {
		this(projection, Bundle.EMPTY);
	}

	private DocumentCursor(String[] projection, Bundle extras) {
		super(projection == null ? DEFAULT_PROJECTION : projection);
		this.extras = extras;
	}

	/**
	 * Creates a cursor that is filled with one {@link Document}.
	 */
	public DocumentCursor(String[] projection, Document document) {
		this(projection, Bundle.EMPTY);

		addRow(document);
	}

	/**
	 * Creates a cursor that is filled with multiple {@link Document}s.
	 */
	public DocumentCursor(String[] projection, Document[] documents, Bundle extras) {
		this(projection,extras);

		for (Document document : documents) {
			addRow(document);
		}
	}

	/**
	 * Adds a new row for this {@link Document}.
	 */
	private void addRow(Document document) {
		newRow().add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, document.name)
			.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, document.documentId)
			.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
			.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, document.lastModified)
			.add(DocumentsContract.Document.COLUMN_MIME_TYPE, document.mimeType)
			.add(DocumentsContract.Document.COLUMN_SIZE, document.size);
	}

	@Override
	public Bundle getExtras() {
		return extras;
	}

}
