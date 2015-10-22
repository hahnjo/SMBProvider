package de.hahnjo.android.smbprovider.provider.cursor;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;

/**
 * A {@link Cursor} that tells the client of a {@link DocumentsProvider} that additional network requests have to be done. This shows an animated
 * loading circle to the user.
 */
public class ExtraLoadingCursor extends AbstractCursor {

	public static final Bundle EXTRA_LOADING_BUNDLE = new Bundle();
	static {
		EXTRA_LOADING_BUNDLE.putBoolean(DocumentsContract.EXTRA_LOADING, true);
	}

	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public String[] getColumnNames() {
		return new String[0];
	}

	@Override
	public String getString(int column) {
		return null;
	}

	@Override
	public short getShort(int column) {
		return 0;
	}

	@Override
	public int getInt(int column) {
		return 0;
	}

	@Override
	public long getLong(int column) {
		return 0;
	}

	@Override
	public float getFloat(int column) {
		return 0;
	}

	@Override
	public double getDouble(int column) {
		return 0;
	}

	@Override
	public boolean isNull(int column) {
		return false;
	}

	@Override
	public Bundle getExtras() {
		return EXTRA_LOADING_BUNDLE;
	}
}
