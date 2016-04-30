package net.heatherandkevin.motowatchface.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import net.heatherandkevin.motowatchface.Test;
import net.heatherandkevin.motowatchface.service.BatteryService;
import net.heatherandkevin.motowatchface.service.WeatherService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    public static final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(ACTION_BOOT)) {
            Log.i(TAG, "Received Boot Intent");
            SharedPreferences prefs = context.getSharedPreferences("net.heatherandkevin.motowatchface_preferences", Context.MODE_PRIVATE);
            //check battery
            if (prefs.getBoolean(Test.BATTERY_KEY,false)) {
                Intent monitorIntent = new Intent(context, BatteryService.class);
                monitorIntent.putExtra(BatteryService.BATTERY_UPDATE, true);
                Log.i(TAG, "Starting Battery Receiver");
                context.startService(monitorIntent);
            } else {
                Log.i(TAG, "Battery Service is not on");
            }

            //check weather
            if (prefs.getBoolean(Test.WEATHER_KEY,false)) {
                Intent monitorIntent = new Intent(context, WeatherService.class);
                monitorIntent.putExtra(WeatherService.WEATHER_UPDATE, true);
                Log.i(TAG, "Starting Weather Receiver");
                context.startService(monitorIntent);
            } else {
                Log.i(TAG, "Weather Service is not on");
            }
        }
    }
}
