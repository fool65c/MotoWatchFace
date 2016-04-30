package net.heatherandkevin.motowatchface;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import net.heatherandkevin.motowatchface.receivers.BatteryAlarmReceiver;
import net.heatherandkevin.motowatchface.receivers.WeatherAlarmReceiver;

public class Test extends Activity {
    private static final String TAG="KMAGER TEST ACTIVITY";

    public static final String BATTERY_KEY = "battery_switch";
    public static final String WEATHER_KEY = "weather_switch";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
    }


    public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final int REQUEST_CODE_ASK_PERMISSIONS = 6565;

        @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                // Load the preferences from an XML resource
                addPreferencesFromResource(R.xml.pref_general);

            }

            @Override
            public void onResume() {
                super.onResume();
                // Set up a listener whenever a key changes
                getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            }

            @Override
            public void onPause() {
                super.onPause();
                // Set up a listener whenever a key changes
                getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            }

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                // just update all
                Log.i("KMAGER",key);
                Log.i("KMAGER", ":" + sharedPreferences.getBoolean(key, false));

                if (key.equals(BATTERY_KEY)) {
                    if (sharedPreferences.getBoolean(key,false)) {
                        BatteryAlarmReceiver.startAlarms(getContext());
                    } else {
                        BatteryAlarmReceiver.stopAlarms(getContext());
                    }
                }

                if (key.equals(WEATHER_KEY)) {
                    if (sharedPreferences.getBoolean(key,false)) {
                        requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_ASK_PERMISSIONS);
                        WeatherAlarmReceiver.startAlarms(getContext());
                    } else {
                        WeatherAlarmReceiver.stopAlarms(getContext());
                    }
                }
            }
    }
}
