package de.hahnjo.android.smbprovider;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.hahnjo.android.smbprovider.account.SMBAccountAuthenticator;

/**
 * The {@link Activity} that allows the user to setup a new {@link Account}.
 */
public class AccountSetupActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

	private static final String TAG = AccountSetupActivity.class.getSimpleName();

	private static final String INTENT_SHOW_HOME_AS_UP = "showHomeAsUp";

	private MenuItem addAccount;

	private EditTextPreference name;
	private EditTextPreference server;
	private EditTextPreference username;
	private EditTextPreference domain;
	private EditTextPreference password;
	private Preference status;

	@Override
	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.account_setup_preferences);

		name = (EditTextPreference) findPreference(getString(R.string.preferences_name_key));
		server = (EditTextPreference) findPreference(getString(R.string.preferences_server_key));
		username = (EditTextPreference) findPreference(getString(R.string.preferences_username_key));
		domain = (EditTextPreference) findPreference(getString(R.string.preferences_domain_key));
		password = (EditTextPreference) findPreference(getString(R.string.preferences_password_key));
		status = findPreference(getString(R.string.preferences_status_key));

		name.setOnPreferenceChangeListener(this);
		server.setOnPreferenceChangeListener(this);
		username.setOnPreferenceChangeListener(this);
		domain.setOnPreferenceChangeListener(this);
		password.setOnPreferenceChangeListener(this);

		status.setOnPreferenceClickListener(this);

		if (getIntent().getBooleanExtra(INTENT_SHOW_HOME_AS_UP, false)) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!isEmpty(server)) {
			testConnection();
		}
		updatePreferences();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.add_account, menu);
		addAccount = menu.findItem(R.id.menu_addAccount);
		setAddAccountEnabled(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				return true;
			case R.id.menu_addAccount:
				if (!isEmpty(server) && !isEmpty(name)) {
					boolean added = addAccount();
					if (added) {
						finish();
					}
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// The value would be set _AFTER_ this method has finished
		if (preference instanceof EditTextPreference) {
			((EditTextPreference) preference).setText((String) newValue);
		}
		// If some of the data changed that is relevant for the connection, retest it
		if (!isEmpty(server) && (preference == server || preference == username || preference == domain || preference == password)) {
			testConnection();
		}

		updatePreferences();

		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == status) {
			if (!isEmpty(server)) {
				testConnection();
			}
			return true;
		}
		return false;
	}

	/**
	 * Adds the account with the given data. Name and server _MUST NOT_ be empty!
	 * @return true, if the account was added and the activity can be finished
	 */
	private boolean addAccount() {
		if (BuildConfig.DEBUG) Log.d(TAG, "addAccount");

		String name = this.name.getText();
		// The name must be unique!
		if (!SMBAccountAuthenticator.isUniqueAccountName(getApplicationContext(), name)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.invalidName_title)
				.setMessage(getString(R.string.invalidName_message, name))
				.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
			builder.show();
			return false;
		} else if (name.contains("/")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.slashInName_title)
				.setMessage(R.string.slashInName_message)
				.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
			builder.show();
			return false;
		}

		Bundle options = new Bundle();
		options.putString(SMBAccountAuthenticator.ACCOUNT_NAME, name);
		options.putString(SMBAccountAuthenticator.ACCOUNT_SERVER, server.getText());

		if (!isEmpty(username)) {
			options.putString(SMBAccountAuthenticator.ACCOUNT_USERNAME, username.getText());

			if (!isEmpty(domain)) {
				options.putString(SMBAccountAuthenticator.ACCOUNT_DOMAIN, domain.getText());
			}

			if (!isEmpty(password)) {
				options.putString(SMBAccountAuthenticator.ACCOUNT_PASSWORD, password.getText());
			}
		}

		SMBAccountAuthenticator.getAccountManager(this).addAccount(SMBAccountAuthenticator.ACCOUNT_TYPE, null, null, options, this, null, null);
		return true;
	}

	private boolean isEmpty(EditTextPreference preference) {
		return preference.getText() == null || preference.getText().length() == 0;
	}

	/**
	 * Calls {@link MenuItem#setEnabled(boolean)} if {@link #addAccount} is not null.
	 */
	private void setAddAccountEnabled(boolean enabled) {
		if (addAccount != null) {
			addAccount.setEnabled(enabled);
		}
	}

	private void testConnection() {
		if (BuildConfig.DEBUG) Log.d(TAG, "testConnection");

		if (!isEmpty(server)) {
			new TestConnectionThread().start();
		}
	}

	private void updatePreferences() {
		// Name is required
		if (isEmpty(name)) {
			name.setSummary(R.string.preferences_name_summary);
		} else {
			name.setSummary(name.getText());
		}

		// Server is required
		if (isEmpty(server)) {
			server.setSummary(R.string.preferences_server_summary);
		} else {
			server.setSummary(server.getText());
		}

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
			if (BuildConfig.DEBUG) Log.d(TAG, "TestConnectionThread.run");

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					status.setSummary(R.string.status_connecting);
				}

			});

			SMBConnection connection = new SMBConnection(server.getText(), username.getText(), domain.getText(), password.getText());
			final boolean test = connection.test();

			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if (test) {
						status.setSummary(R.string.status_connected);
						setAddAccountEnabled(!isEmpty(name));
					} else {
						status.setSummary(R.string.status_disconnected);
						setAddAccountEnabled(false);
					}
				}

			});
		}

	}

	/**
	 * Starts this activity. It will then also call {@link ActionBar#setDisplayHomeAsUpEnabled(boolean)}.
	 */
	public static void start(Activity from) {
		Intent intent = new Intent(from, AccountSetupActivity.class);
		intent.putExtra(INTENT_SHOW_HOME_AS_UP, true);
		from.startActivity(intent);
	}

}
