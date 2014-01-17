package de.hahnjo.android.smbprovider.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.DocumentsContract;
import android.util.Log;
import de.hahnjo.android.smbprovider.BuildConfig;

/**
 * A class that manages the connection to the database.
 */
public class DocumentDatabase extends SQLiteOpenHelper {

	private static final String TAG = DocumentDatabase.class.getSimpleName();

	private static final String DATABASE_NAME = "documents.db";
	private static final int DATABASE_VERSION = 1;

	public static final String TABLE_NAME = "documents";
	public class Columns {
		public static final String ID = "_id";

		/**
		 * @see DocumentsContract.Document#COLUMN_DISPLAY_NAME
		 */
		public static final String DISPLAY_NAME = DocumentsContract.Document.COLUMN_DISPLAY_NAME;

		/**
		 * @see DocumentsContract.Document#COLUMN_DOCUMENT_ID
		 */
		public static final String DOCUMENT_ID = DocumentsContract.Document.COLUMN_DOCUMENT_ID;

		/**
		 * @see DocumentsContract.Document#COLUMN_FLAGS
		 */
		public static final String FLAGS = DocumentsContract.Document.COLUMN_FLAGS;

		/**
		 * @see DocumentsContract.Document#COLUMN_LAST_MODIFIED
		 */
		public static final String LAST_MODIFIED = DocumentsContract.Document.COLUMN_LAST_MODIFIED;

		/**
		 * @see DocumentsContract.Document#COLUMN_MIME_TYPE
		 */
		public static final String MIME_TYPE = DocumentsContract.Document.COLUMN_MIME_TYPE;

		/**
		 * @see DocumentsContract.Document#COLUMN_SIZE
		 */
		public static final String SIZE = DocumentsContract.Document.COLUMN_SIZE;
	}

	public DocumentDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		if (BuildConfig.DEBUG) Log.d(TAG, "onCreate");

		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + Columns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Columns.DISPLAY_NAME + " VARCHAR, " +
			Columns.DOCUMENT_ID + " VARCHAR, " + Columns.FLAGS + " INTEGER, " + Columns.LAST_MODIFIED + " INTEGER, " +
			Columns.MIME_TYPE + " VARCHAR, " + Columns.SIZE + " INTEGER)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (BuildConfig.DEBUG) Log.d(TAG, "onUpgrade");
	}

}
