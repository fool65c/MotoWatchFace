package net.heatherandkevin.motowatchface.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import net.heatherandkevin.motowatchface.domain.Weather;
import net.heatherandkevin.motowatchface.receivers.WeatherAlarmReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class WeatherService extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "WEATHER_SERVICE";
    public static final String WEATHER_UPDATE = "weather";
    private static final String WEATHER_URI = "/WEATHER_STATS";

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, " google client API connected");
        new WeatherCheckAsync().execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, " google client API suspended");
        this.stopSelf();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, " google client API failed: " + connectionResult.getErrorMessage());
        this.stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting Service");

        if (intent != null && intent.hasExtra(WEATHER_UPDATE)){
            //Connect the GoogleApiClient
            mGoogleApiClient = new GoogleApiClient.Builder(WeatherService.this.getApplicationContext())
                    .addApi(Wearable.API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();
            WeatherAlarmReceiver.startAlarms(WeatherService.this.getApplicationContext());
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class WeatherCheckAsync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... arg0) {
            Location location = getLocation();

            if (location != null) {
                try {
                    Weather weather = getWeather(location.getLatitude(), location.getLongitude());
                    updateWear(weather);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.w(TAG,"The location is null");
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){
            WeatherService.this.stopSelf();
        }

        private Location getLocation() {
            Location location;
            int permissionCheck = ContextCompat.checkSelfPermission(WeatherService.this,
                    Manifest.permission.ACCESS_FINE_LOCATION);

            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            } else {
                Log.w(TAG,"We do now have ACCESS_FINE_LOCATION permissions");
                location = null;
            }

            return location;
        }

        private Weather getWeather(double lat, double lon) throws JSONException {
            JSONObject data = ( (new WeatherHttpClient()).getWeatherData(lat,lon));

            return new Weather(data);
        }

        private void updateWear(Weather weather) {
            final PutDataMapRequest putRequest = PutDataMapRequest.create(WEATHER_URI);
            putRequest.getDataMap().putLong("timestamp", System.currentTimeMillis());
            putRequest.getDataMap().putString("icon", weather.getIcon());
            putRequest.getDataMap().putFloat("temp", weather.getTemp());
            putRequest.getDataMap().putFloat("high", weather.getTempHigh());
            putRequest.getDataMap().putFloat("low", weather.getTempLow());
            putRequest.getDataMap().putLong("sunrise", weather.getSunrise());
            putRequest.getDataMap().putLong("sunset", weather.getSunset());
            Wearable.DataApi.putDataItem(mGoogleApiClient, putRequest.asPutDataRequest());
            mGoogleApiClient.disconnect();
        }
    }
}