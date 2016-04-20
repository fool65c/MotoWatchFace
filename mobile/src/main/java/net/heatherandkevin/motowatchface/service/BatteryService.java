package net.heatherandkevin.motowatchface.service;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import net.heatherandkevin.motowatchface.Test;

import java.util.Date;

public class BatteryService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    public static String SERVICE_CALLED_WEAR = "WearRequestBatteryLevel";

    public BatteryService() {
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        String event = messageEvent.getPath();

        Log.d("KMAGER", event);

        String [] message = event.split("--");

        if (message[0].equals(SERVICE_CALLED_WEAR)) {
            Log.i("KMAGER", "CALLING UPDATE BATTERY STATS");

            //GoogleApi
            //Connect the GoogleApiClient
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!mResolvingError) {
                mGoogleApiClient.connect();
                updateBatteryStats();
            }
        }
    }

    private void updateBatteryStats() {
        Float batteryLevel;

        IntentFilter batteryStatusIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = this.getApplicationContext().registerReceiver(null, batteryStatusIntentFilter);

        if (batteryStatusIntent != null) {
            int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryLevel = level / (float) scale;
            Log.i("KMAGER", "Battery Level: " + batteryLevel);
            sendMessage(batteryLevel);
        } else {
            Log.i("KMAGER", "CAN NOT GET BATTERY LEVEL");
        }
    }

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

    /**
     * Send message to mobile handheld
     */
    private void sendMessage(Float key) {
        if (mGoogleApiClient==null){
            return;
        }

        Log.i("KMAGER", "IS THIS WORKING???" + key);

        final PutDataMapRequest putRequest = PutDataMapRequest.create("/SAMPLE");
        putRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());
        putRequest.getDataMap().putFloat("BatteryLevel", key);
        Wearable.DataApi.putDataItem(mGoogleApiClient, putRequest.asPutDataRequest());

//        if (mNode != null && mGoogleApiClient!= null && mGoogleApiClient.isConnected()) {
//            Log.d("WEAR", "-- " + mGoogleApiClient.isConnected());
//            Wearable.MessageApi.sendMessage(
//                    mGoogleApiClient, mNode.getId(), SERVICE_BATTERY_LEVEL + "--" + Key, null).setResultCallback(
//
//                    new ResultCallback<MessageApi.SendMessageResult>() {
//                        @Override
//                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
//
//                            if (!sendMessageResult.getStatus().isSuccess()) {
//                                Log.e("WEAR", "Failed to send message with status code: "
//                                        + sendMessageResult.getStatus().getStatusCode());
//                            }
//                        }
//                    }
//            );
//        }

    }
}
