package net.heatherandkevin.motowatchface.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import net.heatherandkevin.motowatchface.receivers.BatteryAlarmReceiver;

public class BatteryService extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "BATTERY_SERVICE";
    public static final String BATTERY_UPDATE = "battery";
    private static final String BATTERY_URI = "/BATTERY_LEVEL";

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, " google client API connected");
        new BatteryCheckAsync().execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, " google client API suspended");
        this.stopSelf();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, " google client API failed: " + connectionResult.getErrorMessage());
        mGoogleApiClient.disconnect();
        this.stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting Service");

        if (intent != null && intent.hasExtra(BATTERY_UPDATE)){
            //Connect the GoogleApiClient
            mGoogleApiClient = new GoogleApiClient.Builder(BatteryService.this.getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
            BatteryAlarmReceiver.startAlarms(BatteryService.this.getApplicationContext());
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class BatteryCheckAsync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... arg0) {
            float batteryLevel = getBatteryLevel();
            Log.i(TAG,"Battery Level: " + batteryLevel);
            updateWear(batteryLevel);
            mGoogleApiClient.disconnect();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){
            BatteryService.this.stopSelf();
        }

        private float getBatteryLevel(){
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = BatteryService.this.registerReceiver(null, ifilter);
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return level / (float)scale;
        }

        private void updateWear(float batteryLevel) {
            final PutDataMapRequest putRequest = PutDataMapRequest.create(BATTERY_URI);
            putRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());
            putRequest.getDataMap().putFloat("BatteryLevel", batteryLevel);
            Wearable.DataApi.putDataItem(mGoogleApiClient, putRequest.asPutDataRequest());
        }
    }
}
