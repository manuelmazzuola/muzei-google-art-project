package com.manuelmazzuola.muzeigoogleartproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;

public class SettingsActivity extends ActionBarActivity {
    private static final String DELAY_PREF = "muzeigap.delay";
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);

        Toolbar tb = ((Toolbar) findViewById(R.id.toolbar));
        setSupportActionBar(tb);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if(key.equals(DELAY_PREF)) {
                    String configFreq = prefs.getString(DELAY_PREF, Integer.toString(R.string.default_delay));
                    Intent intent = new Intent(getApplicationContext(), GoogleArtProjectSource.class);
                    intent.putExtra("configFreq", configFreq);
                    startService(intent);
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }
}