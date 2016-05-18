package net.heatherandkevin.motowatchface.service;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import net.heatherandkevin.motowatchface.MotoWatchFaceActivity;

/**
 * Created by kmager on 5/17/16.
 * Opens the main activity
 */
public class ListenerServiceFromWear extends WearableListenerService {
    private static final String OPEN_MAIN_ACTIVITY_PATH = "/openMainActivity";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        /*
         * Receive the message from wear
         */
        if (messageEvent.getPath().equals(OPEN_MAIN_ACTIVITY_PATH)) {

            //For example you can start an Activity
            Intent startIntent = new Intent(this, MotoWatchFaceActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }

    }

}
