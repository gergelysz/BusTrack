package bustracker.ms.sapientia.ro.bustrack.Fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import bustracker.ms.sapientia.ro.bustrack.R;

public class SettingsFragment extends PreferenceFragment {

    public static final String DARK_MAP_THEME = "switch_theme";

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        preferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals(DARK_MAP_THEME)) {
                boolean isDarkModeChecked = sharedPreferences.getBoolean(DARK_MAP_THEME, false);
                if (isDarkModeChecked) {
                    Toast.makeText(getActivity(), "Dark", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Light", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}
