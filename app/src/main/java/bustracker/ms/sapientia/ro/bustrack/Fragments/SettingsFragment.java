package bustracker.ms.sapientia.ro.bustrack.Fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import bustracker.ms.sapientia.ro.bustrack.R;

public class SettingsFragment extends PreferenceFragment {

    public static final String DARK_MAP_THEME = "switch_theme";
    public static final String CURRENT_LOCATION_FOCUS = "switch_current_location";
    public static final String UPDATE_FREQUENCY = "list_update_frequency";
    public static final String UPDATE_PRIORITY = "list_update_priority";

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        preferenceChangeListener = (sharedPreferences, key) -> {
            switch (key) {
                case DARK_MAP_THEME:
                    boolean isDarkModeChecked = sharedPreferences.getBoolean(DARK_MAP_THEME, false);
                    if (isDarkModeChecked) {
                        Toast.makeText(getActivity(), "Dark", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Light", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case CURRENT_LOCATION_FOCUS:
                    boolean isCurrentLocationFocusChecked = sharedPreferences.getBoolean(CURRENT_LOCATION_FOCUS, false);
                    if (isCurrentLocationFocusChecked) {
                        Toast.makeText(getActivity(), "Focus on", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Focus off", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case UPDATE_FREQUENCY:
//                    Toast.makeText(getActivity(), sharedPreferences.getString(UPDATE_FREQUENCY, "5000"), Toast.LENGTH_SHORT).show();
                    Toast.makeText(getActivity(), "You'll need to restart the app for the changes to work.", Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_PRIORITY:
//                    Toast.makeText(getActivity(), sharedPreferences.getString(UPDATE_PRIORITY, "0"), Toast.LENGTH_SHORT).show();
                    Toast.makeText(getActivity(), "You'll need to restart the app for the changes to work.", Toast.LENGTH_SHORT).show();
                    break;
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
