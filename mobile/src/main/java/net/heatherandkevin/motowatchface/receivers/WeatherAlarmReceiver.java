package net.heatherandkevin.motowatchface.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import net.heatherandkevin.motowatchface.service.BatteryService;
import net.heatherandkevin.motowatchface.service.WeatherService;

public class WeatherAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "WeatherAlarmReceiver";
    private static final int REQUEST_CODE = 777;
    public static final long ALARM_INTERVAL = AlarmManager.INTERVAL_HALF_HOUR;


    // Call to schedule future alarms
    public static void startAlarms(final Context context) {
        Log.i(TAG, "Scheduling future alarm");
        final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // start alarm right away
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + ALARM_INTERVAL,
                getAlarmIntent(context));
    }

    public static void stopAlarms(final Context context) {
        Log.i("cancelAlarm", "Canceling Next schedule alarm");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getAlarmIntent(context));
    }

    /*
     * Creates the PendingIntent used for alarms of this receiver.
     */
    private static PendingIntent getAlarmIntent(final Context context) {
        return PendingIntent.getBroadcast(context, REQUEST_CODE, new Intent(context, WeatherAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (context == null) {
            // Somehow you've lost your context; this really shouldn't happen
            return;
        }
        if (intent == null){
            // No intent was passed to your receiver; this also really shouldn't happen
            return;
        }
        if (intent.getAction() == null) {
            // If you called your Receiver explicitly, this is what you should expect to happen
            Intent monitorIntent = new Intent(context, WeatherService.class);
            monitorIntent.putExtra(WeatherService.WEATHER_UPDATE, true);
            context.startService(monitorIntent);
        }
    }
}
