package bustracker.ms.sapientia.ro.bustrack.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Objects;

import bustracker.ms.sapientia.ro.bustrack.fragments.SettingsFragment;
import bustracker.ms.sapientia.ro.bustrack.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Settings");

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            getFragmentManager().beginTransaction().add(R.id.fragment_container, new SettingsFragment()).commit();
        }
    }
}
