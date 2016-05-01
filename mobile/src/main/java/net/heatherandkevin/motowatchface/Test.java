package net.heatherandkevin.motowatchface;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;

import net.heatherandkevin.motowatchface.receivers.BatteryAlarmReceiver;
import net.heatherandkevin.motowatchface.receivers.WeatherAlarmReceiver;
import net.heatherandkevin.motowatchface.service.BatteryService;
import net.heatherandkevin.motowatchface.service.WeatherService;

public class Test extends Activity {
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 6565;

    public static final String BATTERY_KEY = "battery_switch";
    public static final String WEATHER_KEY = "weather_switch";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Request permissions
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_CODE_ASK_PERMISSIONS);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
    }

    public static class PrefsFragment
            extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

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
            if (key.equals(BATTERY_KEY)) {
                if (sharedPreferences.getBoolean(key,false)) {
                    Intent batteryIntent = new Intent(getActivity(), BatteryService.class);
                    batteryIntent.putExtra(BatteryService.BATTERY_UPDATE, true);
                    getActivity().startService(batteryIntent);
                } else {
                    BatteryAlarmReceiver.stopAlarms(getActivity().getApplicationContext());
                }
            }

            if (key.equals(WEATHER_KEY)) {
                if (sharedPreferences.getBoolean(key,false)) {
                    int permissionCheck = ContextCompat.checkSelfPermission(getContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION);
                    if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_CODE_ASK_PERMISSIONS);
                    }
                    Intent weatherIntent = new Intent(getActivity(), WeatherService.class);
                    weatherIntent.putExtra(WeatherService.WEATHER_UPDATE, true);
                    getActivity().startService(weatherIntent);
                } else {
                    WeatherAlarmReceiver.stopAlarms(getActivity().getApplicationContext());
                }
            }
        }
    }
}
