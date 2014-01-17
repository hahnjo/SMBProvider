package de.hahnjo.android.smbprovider;

import java.net.MalformedURLException;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;
import de.hahnjo.android.smbprovider.account.SMBAccountAuthenticator;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

/**
 * A utility class for handling a connection to an SMB server.
 */
public class SMBConnection {

	private static final String TAG = SMBConnection.class.getSimpleName();

	private static final String BASE_URL = "smb://%s";

	private final String server;
	private final NtlmPasswordAuthentication authentication;

	/**
	 * Creates a new connection that gets its details from the given {@link Account}.
	 */
	public SMBConnection(Context context, Account account) {
		this(SMBAccountAuthenticator.getAccountServer(context, account), SMBAccountAuthenticator.getAccountUsername(context, account),
			SMBAccountAuthenticator.getAccountDomain(context, account), SMBAccountAuthenticator.getAccountPassword(context, account));
	}

	/**
	 * Creates a new connection with the given details.
	 */
	public SMBConnection(String server, String username, String domain, String password) {
		this.server = server;
		if (username == null || username.length() == 0) {
			authentication = NtlmPasswordAuthentication.ANONYMOUS;
		} else {
			authentication = new NtlmPasswordAuthentication(domain, username, password);
		}
	}

	/**
	 * @return the opened {@link SmbFile} that points to that path
	 */
	public SmbFile openPath(String path) {
		String url = String.format(BASE_URL, server) + path;
		try {
			return new SmbFile(url, authentication);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Error occurred while opening a path", e);
			return null;
		}
	}

	/**
	 * @return true, if the server is available
	 */
	public boolean test() {
		try {
			SmbFile root = openPath("/");
			return root.exists();
		} catch (Throwable t) {
			Log.e(TAG, "Error occurred while testing the connection", t);
			return false;
		}
	}

}
