package de.hahnjo.android.smbprovider;

import java.util.Collections;
import java.util.List;

import android.accounts.Account;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.hahnjo.android.smbprovider.account.SMBAccountAuthenticator;

/**
 * The default {@link Activity} for this app. It lists all {@link Account}s / roots and allows to delete or edit them and to add a new one.
 *
 * @see AccountSetupActivity
 */
public class AccountPreferences extends PreferenceActivity {

	private static final String TAG = AccountPreferences.class.getSimpleName();

	private AccountTask accountTask;
	private Header[] accountHeaders;

	@Override
	protected void onResume() {
		super.onResume();
		updateAccounts();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		cancelAccountTask();
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		// Always rebuild the headers from scratch
		target.clear();

		// Add the loaded accounts
		if (accountHeaders != null) {
			Collections.addAll(target, accountHeaders);
		}

		// Add a header to show the AboutFragment
		Header aboutHeader = new Header();
		aboutHeader.titleRes = R.string.header_about;
		aboutHeader.fragment = AboutFragment.class.getCanonicalName();
		target.add(aboutHeader);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.add_account, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_addAccount:
				AccountSetupActivity.start(this);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return fragmentName.equals(AboutFragment.class.getCanonicalName()) ||
			fragmentName.equals(AccountDetailFragment.class.getCanonicalName());
	}

	private void cancelAccountTask() {
		if (accountTask != null) {
			accountTask.cancel(true);
			accountTask = null;
		}
	}

	private void updateAccounts() {
		cancelAccountTask();
		accountTask = (AccountTask) new AccountTask().execute();
	}

	private class AccountTask extends AsyncTask<Void, Void, Header[]> {

		@Override
		protected Header[] doInBackground(Void... params) {
			Account[] accounts = SMBAccountAuthenticator.getAccounts(getApplicationContext());
			Header[] headers = new Header[accounts.length];

			for (int i = 0; i < accounts.length; i++) {
				Account account = accounts[i];
				Header header = new Header();

				Bundle arguments = new Bundle();
				arguments.putString(AccountDetailFragment.ARGUMENT_ACCOUNT_NAME, account.name);

				header.id = i;
				header.summary = SMBAccountAuthenticator.getSummary(getApplicationContext(), account);
				header.title = account.name;

				header.fragment = AccountDetailFragment.class.getCanonicalName();
				header.fragmentArguments = arguments;

				headers[i] = header;
			}
			return headers;
		}

		@Override
		protected void onPostExecute(Header[] headers) {
			if (isCancelled() || headers == null) {
				return;
			}

			accountHeaders = headers;
			invalidateHeaders();
			if (BuildConfig.DEBUG) Log.d(TAG, "Loaded " + accountHeaders.length + " account(s)...");
		}
	}

}
