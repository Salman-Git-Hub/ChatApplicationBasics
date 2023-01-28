package com.ali.chatapplicationbasics.settings;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.ali.chatapplicationbasics.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class SettingsActivity extends AppCompatActivity {

    private static LinearProgressIndicator progressIndicator;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        toolbar = findViewById(R.id.toolbar);
        progressIndicator = findViewById(R.id.progress);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        toolbar.setTitle("Settings");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            appPrefs();
        }

        private void appPrefs() {
            Preference.OnPreferenceChangeListener cache_listen = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    try {
                        int val = Integer.parseInt(newValue.toString());
                        if (val > 100 || val < 1) {
                            Toast.makeText(getContext(), "Value must be between 1 and 100", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        Toast.makeText(getContext(), "Requires app to restart!", Toast.LENGTH_SHORT).show();
                        return true;
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Value must be numeric!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            };
            Preference.OnPreferenceChangeListener perst_listen = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    Toast.makeText(getContext(), "App requires complete restart", Toast.LENGTH_SHORT).show();
                    return true;
                }
            };
            Preference.OnPreferenceChangeListener theme_listen = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    AppCompatDelegate.setDefaultNightMode(Integer.parseInt(newValue.toString()));
                    return true;
                }
            };
            Preference cache_pref = findPreference("cache_size");
            Preference perst_perf = findPreference("app_persistence");
            Preference theme_pref = findPreference("pref_theme");

            cache_pref.setOnPreferenceChangeListener(cache_listen);
            perst_perf.setOnPreferenceChangeListener(perst_listen);
            theme_pref.setOnPreferenceChangeListener(theme_listen);
        }


    }
}