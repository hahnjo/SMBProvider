package de.hahnjo.android.smbprovider.provider.cursor;

import android.accounts.Account;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.DocumentsContract;
import de.hahnjo.android.smbprovider.R;
import de.hahnjo.android.smbprovider.account.SMBAccountAuthenticator;

/**
 * A {@link Cursor} that contains information about roots as specified by {@link DocumentsContract.Root}.
 */
public class RootCursor extends MatrixCursor {

	private static final String[] DEFAULT_PROJECTION = new String[] {
		DocumentsContract.Root.COLUMN_DOCUMENT_ID,
		DocumentsContract.Root.COLUMN_FLAGS,
		DocumentsContract.Root.COLUMN_ICON,
		DocumentsContract.Root.COLUMN_MIME_TYPES,
		DocumentsContract.Root.COLUMN_ROOT_ID,
		DocumentsContract.Root.COLUMN_SUMMARY,
		DocumentsContract.Root.COLUMN_TITLE
	};

	/**
	 * Creates a new cursor with information about all roots (specified by {@link Account}s) that are found.
	 */
	public RootCursor(Context context, String[] projection) {
		super(projection == null ? DEFAULT_PROJECTION : projection);

		Account[] accounts = SMBAccountAuthenticator.getAccounts(context);
		for (Account account : accounts) {
			String summary = SMBAccountAuthenticator.getSummary(context, account);

			addRow(account.name, summary);
		}
	}

	private void addRow(String accountName, String summary) {
		newRow().add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, accountName + '/')
			.add(DocumentsContract.Root.COLUMN_FLAGS, 0)
			.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_launcher)
			.add(DocumentsContract.Root.COLUMN_MIME_TYPES, null)
			.add(DocumentsContract.Root.COLUMN_ROOT_ID, accountName)
			.add(DocumentsContract.Root.COLUMN_SUMMARY, summary)
			.add(DocumentsContract.Root.COLUMN_TITLE, accountName);
	}

}
