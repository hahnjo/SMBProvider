package de.hahnjo.android.smbprovider.account;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import de.hahnjo.android.smbprovider.AccountSetupActivity;
import de.hahnjo.android.smbprovider.BuildConfig;

/**
 * An {@link AbstractAccountAuthenticator} for SMB {@link Account}s. Also contains various utility methods concerning accounts.
 */
public class SMBAccountAuthenticator extends AbstractAccountAuthenticator {

	private static final String TAG = SMBAccountAuthenticator.class.getSimpleName();

	public static final String ACCOUNT_TYPE = "de.hahnjo.android.smbprovider";

	public static final String ACCOUNT_NAME = "name";
	public static final String ACCOUNT_SERVER = "server";
	public static final String ACCOUNT_USERNAME = "username";
	public static final String ACCOUNT_DOMAIN = "domain";
	public static final String ACCOUNT_PASSWORD = "password";

	private final Context context;

	public SMBAccountAuthenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		if (BuildConfig.DEBUG) Log.d(TAG, "addAccount");

		if (options != null && options.containsKey(ACCOUNT_NAME) && options.containsKey(ACCOUNT_SERVER)) {
			// The account is given by the options
			String name = options.getString(ACCOUNT_NAME);
			String password = options.getString(ACCOUNT_PASSWORD);

			Account account = new Account(name, ACCOUNT_TYPE);

			Bundle userData = (Bundle) options.clone();
			userData.remove(ACCOUNT_NAME);
			userData.remove(ACCOUNT_PASSWORD);

			getAccountManager(context).addAccountExplicitly(account, password, userData);

			Bundle result = new Bundle();
			result.putString(AccountManager.KEY_ACCOUNT_NAME, name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, ACCOUNT_TYPE);
			return result;
		} else {
			// Show the UI so the user can input the data
			Intent intent = new Intent(context, AccountSetupActivity.class);

			Bundle result = new Bundle();
			result.putParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			result.putParcelable(AccountManager.KEY_INTENT, intent);

			return result;
		}
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
		if (BuildConfig.DEBUG) Log.d(TAG, "confirmCredentials");

		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		if (BuildConfig.DEBUG) Log.d(TAG, "editProperties");

		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
		if (BuildConfig.DEBUG) Log.d(TAG, "getAuthToken");

		return null;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType) {
		if (BuildConfig.DEBUG) Log.d(TAG, "getAuthTokenLabel");

		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
		if (BuildConfig.DEBUG) Log.d(TAG, "hasFeatures");

		return null;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
		if (BuildConfig.DEBUG) Log.d(TAG, "updateCredentials");

		return null;
	}

	// ------------------------------------------------------------------------
	// Static methods

	private static AccountManager manager = null;

	/**
	 * @return the cached {@link AccountManager}
	 */
	public static AccountManager getAccountManager(Context context) {
		if (manager == null) {
			manager = AccountManager.get(context);
		}
		return manager;
	}

	/**
	 * @return an array of all {@link Account}s
	 */
	public static Account[] getAccounts(Context context) {
		return getAccountManager(context).getAccountsByType(ACCOUNT_TYPE);
	}

	/**
	 * @return the {@link Account} with the given name (case-insensitive) or null, if it was not found
	 */
	public static Account getAccount(Context context, String name) {
		Account[] accounts = getAccounts(context);
		for (Account account : accounts) {
			if (account.name.equalsIgnoreCase(name)) {
				return account;
			}
		}

		return null;
	}

	/**
	 * @return this account's user data with the given key
	 */
	private static String getAccountUserData(Context context, Account account, String key) {
		return getAccountManager(context).getUserData(account, key);
	}

	/**
	 * @return this account's server
	 */
	public static String getAccountServer(Context context, Account account) {
		return getAccountUserData(context, account, ACCOUNT_SERVER);
	}

	/**
	 * @return this account's username
	 */
	public static String getAccountUsername(Context context, Account account) {
		return getAccountUserData(context, account, ACCOUNT_USERNAME);
	}

	/**
	 * @return this account's domain
	 */
	public static String getAccountDomain(Context context, Account account) {
		return getAccountUserData(context, account, ACCOUNT_DOMAIN);
	}

	/**
	 * @return this account's password
	 */
	public static String getAccountPassword(Context context, Account account) {
		return getAccountManager(context).getPassword(account);
	}

	/**
	 * @return a summary for this account or null, if it would be the same as its name
	 */
	public static String getSummary(Context context, Account account) {
		StringBuilder builder = new StringBuilder();

		String username = getAccountUsername(context, account);
		if (username != null) {
			builder.append(username).append('@');
		}
		builder.append(getAccountServer(context, account));

		String summary = builder.toString();
		if (account.name.equalsIgnoreCase(summary)) {
			return null;
		}
		return summary;
	}

	/**
	 * @return true, if there is no other {@link Account} with this name (case-insensitive)
	 */
	public static boolean isUniqueAccountName(Context context, String name) {
		return getAccount(context, name) == null;
	}

}
