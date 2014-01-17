package de.hahnjo.android.smbprovider.account;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import de.hahnjo.android.smbprovider.BuildConfig;

/**
 * An {@link Service} that returns the {@link SMBAccountAuthenticator}.
 */
public class AuthenticatorService extends Service {

	private static final String TAG = AuthenticatorService.class.getSimpleName();

	private SMBAccountAuthenticator authenticator;

	@Override
	public void onCreate() {
		if (BuildConfig.DEBUG) Log.d(TAG, "onCreate");

		super.onCreate();
		authenticator = new SMBAccountAuthenticator(getApplicationContext());
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (BuildConfig.DEBUG) Log.d(TAG, "onBind");

		return authenticator.getIBinder();
	}
}
