package net.heatherandkevin.motowatchface;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import net.heatherandkevin.motowatchface.receivers.BatteryReceiver;

public class Test extends Activity {
    private static final String BATTERY_INTENT_ACTION = "net.heatherandkevin.motowatchface.PERIODIC_TASK_BATTERY";
    private static final String BATTERY_INTENT_CANCEL  = "net.heatherandkevin.motowatchface.PERIODIC_TASK_CANCEL";
    private static final String BATTERY_KEY = "battery_switch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
    }


    public static class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
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
                        startBatteryReceiver();
                    } else {
                        stopBatteryReceiver();
                    }
                }
            }

        private void startBatteryReceiver() {
            Intent intent = new Intent(BATTERY_INTENT_ACTION);
            this.getActivity().sendBroadcast(intent);
        }

        private void stopBatteryReceiver() {
            Intent intent = new Intent(BATTERY_INTENT_CANCEL);
            this.getActivity().sendBroadcast(intent);
        }
    }
}
