package net.heatherandkevin.motowatchface.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

public class BatteryReceiver
        extends BroadcastReceiver
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    private static final String INTENT_ACTION = "net.heatherandkevin.motowatchface.PERIODIC_TASK_BATTERY";
    private static final String INTENT_CANCEL  = "net.heatherandkevin.motowatchface.PERIODIC_TASK_CANCEL";
    private static final int INTERVAL_MILLIS = 600;
    private static final String BATTERY_KEY = "battery_switch";

    public BatteryReceiver() {}

    /**
     * Begin GoogleApiClient operations
     */
    Node mNode; // the connected device to send the message to
    GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError=false;
    public static String SERVICE_BATTERY_LEVEL = "MobileBatteryLevel";
    @Override
    public void onConnected(Bundle bundle) {
        resolveNode();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * Resolve the node = the connected device to send the message to
     */
    private void resolveNode() {

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            mNode = node;
                        }
                    }
                });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case INTENT_ACTION:
                Log.i("KMAGER", intent.toString());
                updateBatteryStats(context);
                scheduleAlarm(context);
                break;
            case INTENT_CANCEL:
            default:
                cancelAlarm(context);
                break;
        }
    }

    private void updateBatteryStats(Context context) {
        //GoogleApi
        //Connect the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }

        Float batteryLevel;
        IntentFilter batteryStatusIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = context.registerReceiver(null, batteryStatusIntentFilter);

        if (batteryStatusIntent != null) {
            //get the battery info
            int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryLevel = level / (float) scale;
            Log.i("KMAGER", "Battery Level: " + batteryLevel);

            //send the request
            final PutDataMapRequest putRequest = PutDataMapRequest.create("/SAMPLE");
            putRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());
            putRequest.getDataMap().putFloat("BatteryLevel", batteryLevel);
            Wearable.DataApi.putDataItem(mGoogleApiClient, putRequest.asPutDataRequest());
        } else {
            Log.w("KMAGER", "CAN NOT GET BATTERY LEVEL");
        }
    }

    private void scheduleAlarm(Context context) {
        //Make sure we should schedule the next call
        SharedPreferences prefs = context.getSharedPreferences("net.heatherandkevin.motowatchface_preferences", Context.MODE_PRIVATE);
        Log.i("KMAGER", "COMMON: " + prefs.getBoolean(BATTERY_KEY, false));
        if (prefs.getBoolean(BATTERY_KEY,false)) {
            Intent alarmIntent = new Intent(context, BatteryReceiver.class);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmIntent.setAction(INTENT_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    INTERVAL_MILLIS,
                    pendingIntent);
        } else {
            Log.i(BatteryReceiver.class.toString(),"Call to receiver ignored because preference has been turned off");
        }
    }

    private void cancelAlarm(Context context) {
        Log.i("cancelAlarm","Canceling Next schedule alarm");
        Intent alarmIntent = new Intent(context, BatteryReceiver.class);
        alarmIntent.setAction(INTENT_ACTION);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
