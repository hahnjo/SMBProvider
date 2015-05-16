package de.hahnjo.android.smbprovider;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * A {@link Fragment} that shows some information about this app.
 */
public class AboutFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

	public static final String GOOGLE_PLAY_URI_START = "market://details?id=";
	public static final String GITHUB_URL = "https://github.com/hahnjo/SMBProvider";
	public static final String CONTACT_MAIL_URL = "mailto:Jonas Hahnfeld <smbprovider@hahnjo.de>";

	private Preference googlePlay;
	private Preference github;
	private Preference contact;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.about_preferences);

		googlePlay = findPreference(getString(R.string.about_googlePlay_key));
		github = findPreference(getString(R.string.about_github_key));
		contact = findPreference(getString(R.string.about_contact_key));

		googlePlay.setOnPreferenceClickListener(this);
		github.setOnPreferenceClickListener(this);
		contact.setOnPreferenceClickListener(this);

		Preference version = findPreference(getString(R.string.about_version_key));
		version.setSummary(BuildConfig.VERSION_NAME);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == googlePlay) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_URI_START + BuildConfig.APPLICATION_ID));
			startActivity(intent);
		} else if (preference == github) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
			startActivity(intent);
		} else if (preference == contact) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(CONTACT_MAIL_URL));
			startActivity(intent);
		} else {
			return false;
		}

		return true;
	}
}
