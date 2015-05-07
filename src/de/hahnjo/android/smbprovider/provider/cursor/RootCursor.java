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
			.add(DocumentsContract.Root.COLUMN_FLAGS, 16)
				// 16 (formally DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD) advertises support
				// for directory selection via ACTION_OPEN_DOCUMENT_TREE.  But using the formal
				// name here might require adding <uses-sdk minSdkVersion='21'/> to manifest.
			.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_launcher)
			.add(DocumentsContract.Root.COLUMN_MIME_TYPES, null)
			.add(DocumentsContract.Root.COLUMN_ROOT_ID, accountName)
			.add(DocumentsContract.Root.COLUMN_SUMMARY, summary)
			.add(DocumentsContract.Root.COLUMN_TITLE, accountName);
	}

}
