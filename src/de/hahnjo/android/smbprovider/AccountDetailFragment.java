package de.hahnjo.android.smbprovider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import de.hahnjo.android.smbprovider.account.SMBAccountAuthenticator;

/**
 * A {@link Fragment} that shows data about a single account. It also allows to edit this data.
 */
public class AccountDetailFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener,
	Preference.OnPreferenceClickListener {

	private static final String TAG = AccountDetailFragment.class.getSimpleName();

	public static final String ARGUMENT_ACCOUNT_NAME = "accountName";

	private Account account;

	private EditTextPreference server;
	private EditTextPreference username;
	private EditTextPreference domain;
	private EditTextPreference password;
	private Preference status;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.account_detail_fragment);
		setHasOptionsMenu(true);

		String accountName = getArguments().getString(ARGUMENT_ACCOUNT_NAME);
		account = SMBAccountAuthenticator.getAccount(getActivity().getApplicationContext(), accountName);

		Preference name = findPreference(getString(R.string.preferences_name_key));
		name.setSummary(accountName);

		server = (EditTextPreference) findPreference(getString(R.string.preferences_server_key));
		username = (EditTextPreference) findPreference(getString(R.string.preferences_username_key));
		domain = (EditTextPreference) findPreference(getString(R.string.preferences_domain_key));
		password = (EditTextPreference) findPreference(getString(R.string.preferences_password_key));
		status = findPreference(getString(R.string.preferences_status_key));

		server.setText(SMBAccountAuthenticator.getAccountServer(getActivity(), account));
		username.setText(SMBAccountAuthenticator.getAccountUsername(getActivity(), account));
		domain.setText(SMBAccountAuthenticator.getAccountDomain(getActivity(), account));
		password.setText(SMBAccountAuthenticator.getAccountPassword(getActivity(), account));

		server.setOnPreferenceChangeListener(this);
		username.setOnPreferenceChangeListener(this);
		domain.setOnPreferenceChangeListener(this);
		password.setOnPreferenceChangeListener(this);

		status.setOnPreferenceClickListener(this);

		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		testConnection();
		updatePreferences();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.account_details, menu);
	}

	@Override
	@SuppressWarnings("fallthrough")
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_deleteAccount:
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
					.setTitle(R.string.deleteAccount_title)
					.setMessage(R.string.deleteAccount_message)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							deleteAccount();
						}
					})
					.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
				builder.show();
				return true;
			case R.id.menu_saveAccount:
				saveAccountData();
				// fall through
			case android.R.id.home:
				getActivity().onBackPressed();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == server && (newValue == null || ((String) newValue).length() == 0)) {
			return false;
		}
		// The value would be set _AFTER_ this method has finished
		if (preference instanceof EditTextPreference) {
			((EditTextPreference) preference).setText((String) newValue);
		}

		testConnection();
		updatePreferences();

		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == status) {
			testConnection();
			return true;
		}
		return false;
	}

	private boolean isEmpty(EditTextPreference preference) {
		return preference.getText() == null || preference.getText().length() == 0;
	}

	@SuppressWarnings("deprecation")
	private void deleteAccount() {
		if (BuildConfig.DEBUG) Log.d(TAG, "deleteAccount");

		AccountManager manager = SMBAccountAuthenticator.getAccountManager(getActivity().getApplicationContext());
		manager.removeAccount(account, new AccountManagerCallback<Boolean>() {

			@Override
			public void run(AccountManagerFuture<Boolean> future) {
				getActivity().onBackPressed();
			}

		}, new Handler());
	}

	private void saveAccountData() {
		if (BuildConfig.DEBUG) Log.d(TAG, "saveAccountData");

		AccountManager manager = SMBAccountAuthenticator.getAccountManager(getActivity().getApplicationContext());
		manager.setUserData(account, SMBAccountAuthenticator.ACCOUNT_SERVER, server.getText());

		if (!isEmpty(username)) {
			manager.setUserData(account, SMBAccountAuthenticator.ACCOUNT_USERNAME, username.getText());

			if (!isEmpty(domain)) {
				manager.setUserData(account, SMBAccountAuthenticator.ACCOUNT_DOMAIN, domain.getText());
			} else {
				manager.setUserData(account, SMBAccountAuthenticator.ACCOUNT_DOMAIN, null);
			}

			if (!isEmpty(password)) {
				manager.setPassword(account, password.getText());
			} else {
				manager.clearPassword(account);
			}
		} else {
			manager.setUserData(account, SMBAccountAuthenticator.ACCOUNT_USERNAME, null);
			manager.setUserData(account, SMBAccountAuthenticator.ACCOUNT_DOMAIN, null);
			manager.setUserData(account, SMBAccountAuthenticator.ACCOUNT_PASSWORD, null);
		}
	}

	private void testConnection() {
		if (BuildConfig.DEBUG) Log.d(TAG, "testConnection");

		if (!isEmpty(server)) {
			new TestConnectionThread().start();
		}
	}

	private void updatePreferences() {
		// Server is always set!
		server.setSummary(server.getText());

		// No username means no authentication at all
		if (isEmpty(username)) {
			username.setSummary(R.string.noAuthentication);

			domain.setEnabled(false);
			domain.setSummary(R.string.noAuthentication);

			password.setEnabled(false);
			password.setSummary(R.string.noAuthentication);
		} else {
			username.setSummary(username.getText());

			// Domain is optional
			domain.setEnabled(true);
			if (isEmpty(domain)) {
				domain.setSummary(R.string.noDomain);
			} else {
				domain.setSummary(domain.getText());
			}

			// Password is optional
			password.setEnabled(true);
			if (isEmpty(password)) {
				password.setSummary(R.string.noPassword);
			} else {
				password.setSummary("****");
			}
		}
	}

	private class TestConnectionThread extends Thread {

		@Override
		public void run() {
			if (BuildConfig.DEBUG) Log.d(TAG, "TestConnectionThread.testConnection");

			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					status.setSummary(R.string.status_connecting);
				}

			});

			SMBConnection connection = new SMBConnection(server.getText(), username.getText(), domain.getText(), password.getText());
			final boolean test = connection.test();

			getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (test) {
						status.setSummary(R.string.status_connected);
					} else {
						status.setSummary(R.string.status_disconnected);
					}
				}

			});
		}

	}

}
