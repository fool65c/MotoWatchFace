/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.heatherandkevin.motowatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import net.heatherandkevin.motowatchface.clockhand.AccentHand;
import net.heatherandkevin.motowatchface.clockhand.ClockHand;
import net.heatherandkevin.motowatchface.clockhand.MainHand;
import net.heatherandkevin.motowatchface.domain.WatchFaceWeather;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class MotoWatchFace extends CanvasWatchFaceService implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    /**
     * Begin GoogleApiClient operations
     */
    GoogleApiClient mGoogleApiClient;
    private static final String BATTERY_URI = "/BATTERY_LEVEL";
    float mobileBatteryPercent = 0;

    private static final String WEATHER_URI = "/WEATHER_STATS";
    private WatchFaceWeather weather;


    @Override
    public void onConnected(Bundle bundle) {
        Log.d("KMAGER","data api connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("KMAGER","data api suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.w("KMAGER", "ERROR Connecting:" + connectionResult.getErrorMessage());
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        for (DataEvent event : events) {
            final DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

            switch(event.getDataItem().getUri().getPath()) {
                case BATTERY_URI:
                    Log.i("KMAGER", "Phone Battery Level: " + map.getFloat("BatteryLevel"));
                    mobileBatteryPercent = map.getFloat("BatteryLevel");
                    break;
                case WEATHER_URI:
                    Log.i("KMAGER", "Weather Update");
                    weather = new WatchFaceWeather(map, getResources());

                    break;
                default:
                    Log.w("KMAGER","Unknown path: " + event.getDataItem().getUri().getPath());

            }

        }

    }

    /**
     * END GoogleApiClient operations
     */

    private static class EngineHandler extends Handler {
        private final WeakReference<MotoWatchFace.Engine> mWeakReference;

        public EngineHandler(MotoWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MotoWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        boolean mRegisteredBatteryLevelReceiver = false;

        private float angle;

        /**
         * Bitmap testing
         */
        private Bitmap mBackgroundBitmap;
        private Bitmap mBackgroundAmbientBitmap;
        private Bitmap mBackgroundScaledBitmap;


        /**
         * Tick Mark Configuration
         */
        private float hourTickHeight = 30.0f;

        /**
         * Chin size
         */
        float mChinSize;

        /**
         * Watch Hand Configuration
         */
        private ClockHand hourHand;
        private ClockHand minuteHand;
        private ClockHand secondHand;
        private ClockHand batteryHand;
        private float baseMountWidth = 8f;
        private float baseMountSecondWidth = 4f;
        private float baseMountHole = 2f;
        private float hourHandWidth = 10.0f;
        private float minuteHandWidth = 10.0f;
        private float handOffsetLength = 10f;
        private float secondHandWidth = 2f;
        private float hourHandLengthPercent = 1f / 2.5f;
        private float secondHandLength;

        /**
         * onDraw reusable items
         */
        int faceWidth;
        int faceHeight;
        float xCenter;
        float yCenter;
        double handLength;
        float accessoryCircleSize = 42f;
        float accessoryOffset;

        /**
         * Accessory setup
         * Some are reusable so we only have to create a new instance once
         */
        private Map<Integer,String> weekDays = new HashMap<>(7);
        RectF dayOval;
        RectF weatherStageOval;

        //Setting up paint colors
        Paint mBackgroundPaint;

        Paint mHandPaint;
        Paint mHandBasePaint;
        Paint mHandTipPaint;
        Paint mSecondHandPaint;
        Paint mAccessoryPaint;
        Paint mDayNumberPaint;
        Paint mWeatherPaint;

        Map <Integer,Map<String,Float>> dayLocations;

        boolean mAmbient;

        Calendar calendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                calendar = new GregorianCalendar(TimeZone.getDefault());
            }
        };

        float batteryPercent;
        final BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                batteryPercent = level / (float)scale;
            }
        };

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            //GoogleApi
            //Connect the GoogleApiClient
            mGoogleApiClient = new GoogleApiClient.Builder(MotoWatchFace.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(MotoWatchFace.this)
                    .addOnConnectionFailedListener(MotoWatchFace.this)
                    .build();

            mGoogleApiClient.connect();
            Wearable.DataApi.addListener(mGoogleApiClient, MotoWatchFace.this);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MotoWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = MotoWatchFace.this.getResources();

            Drawable backgroundDrawable = resources.getDrawable(R.drawable.watchface, null);
            Drawable backgroundAmbientDrawable = resources.getDrawable(R.drawable.watchfaceambient, null);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            mBackgroundAmbientBitmap = ((BitmapDrawable) backgroundAmbientDrawable).getBitmap();


            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.handColor));
            mHandPaint.setAntiAlias(true);

            mHandBasePaint= new Paint(mHandPaint);

            mHandTipPaint= new Paint();
            mHandTipPaint.setColor(resources.getColor(R.color.handTipColor));
            mHandTipPaint.setAntiAlias(true);

            mSecondHandPaint = new Paint();
            mSecondHandPaint.setColor(resources.getColor(R.color.secondHandColor));
            mSecondHandPaint.setAntiAlias(true);

            mAccessoryPaint = new Paint();
            mAccessoryPaint.setColor(resources.getColor(R.color.accessoryColor));
            mAccessoryPaint.setStrokeWidth(3);
            mAccessoryPaint.setStyle(Paint.Style.STROKE);
            mAccessoryPaint.setAntiAlias(true);

            hourHand = new MainHand(mHandPaint, mHandTipPaint, hourHandWidth);
            minuteHand = new MainHand(mHandPaint, mHandTipPaint, minuteHandWidth);
            secondHand = new AccentHand(mSecondHandPaint, secondHandWidth, handOffsetLength * 2f);
            batteryHand = new AccentHand(mSecondHandPaint, secondHandWidth, 0f);

            calendar = new GregorianCalendar(TimeZone.getDefault());

            //weekday accessory
            mDayNumberPaint = new Paint();
            mDayNumberPaint.setColor(resources.getColor(R.color.dayColor));
            mDayNumberPaint.setTextAlign(Paint.Align.CENTER);
            mDayNumberPaint.setTextSize(34);
            mDayNumberPaint.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/AC.ttf"));
            mDayNumberPaint.setShadowLayer(5f, 0f, 0f, resources.getColor(R.color.handShadowColor));
            mDayNumberPaint.setAntiAlias(true);

            mWeatherPaint = new Paint(mDayNumberPaint);
            mWeatherPaint.setTextSize(24);

            weekDays.put(7, "SAT");
            weekDays.put(6, "FRI");
            weekDays.put(5, "THU");
            weekDays.put(4, "WED");
            weekDays.put(3, "TUE");
            weekDays.put(2, "MON");
            weekDays.put(1, "SUN");
            dayLocations = calculateDateLocation();

            //THIS STUFF WILL BE DELETED SOON
            DataMap test = new DataMap();
            test.putString("icon", "10n");
            test.putFloat("temp", 49.41f);
            test.putFloat("high",49.41f);
            test.putFloat("low", 49.41f);
            test.putLong("sunrise", 1462010851);
            test.putLong("sunset", 1462060728);
            weather = new WatchFaceWeather(test, getResources());
            //END DELETED STUFF
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mChinSize = insets.getSystemWindowInsetBottom();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                invalidate();
            }

            if (mAmbient && mBackgroundScaledBitmap != null) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundAmbientBitmap,
                        mBackgroundScaledBitmap.getWidth(), mBackgroundScaledBitmap.getHeight(), true /* filter */);
            } else {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        mBackgroundScaledBitmap.getWidth(), mBackgroundScaledBitmap.getHeight(), true /* filter */);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
//                        mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundAmbientBitmap,
                        width, height, true /* filter */);
            }
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            calendar= Calendar.getInstance();

            faceWidth = bounds.width();
            faceHeight = bounds.height();
            xCenter = faceWidth / 2.0f;
            yCenter = faceHeight / 2.0f;

            //draw background
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            //display other information
            if (!isInAmbientMode()) {
                accessoryOffset = (yCenter - hourTickHeight + baseMountWidth) /2f;


                //draw weather
                if ( weatherStageOval == null ) {
                    weatherStageOval = new RectF(xCenter + accessoryOffset - accessoryCircleSize,
                            yCenter - accessoryCircleSize,
                            xCenter + accessoryOffset + accessoryCircleSize,
                            yCenter + accessoryCircleSize);
                }

                if (weather != null){
                    //TODO
                    canvas.drawBitmap(weather.getIconBitMap(),
                            xCenter + accessoryOffset - weather.getIconBitMap().getWidth() / 2f,
                            yCenter - weather.getIconBitMap().getHeight() + 10f,
                            null);

                    canvas.drawText(String.format("%.1f" + (char) 0x00B0, weather.getTemp()),
                            xCenter + accessoryOffset,
                            yCenter + 23,
                            mWeatherPaint);

                    canvas.drawArc(weatherStageOval,
                            -90,
                            -360f * weather.percentLeftInStage(System.currentTimeMillis() / 1000),
                            false,
                            weather.getPercentPaint(System.currentTimeMillis() / 1000));
                }

                //draw week day
                if ( dayOval == null ) {
                    dayOval = new RectF(xCenter - accessoryOffset - accessoryCircleSize + 12f,
                            yCenter - accessoryCircleSize + 9f,
                            xCenter - accessoryOffset + accessoryCircleSize - 9f,
                            yCenter + accessoryCircleSize - 14f);
                }

                //DRAW THE DAY
                canvas.drawArc(dayOval,
                        dayLocations.get(calendar.get(Calendar.DAY_OF_WEEK)).get("start"),
                        dayLocations.get(calendar.get(Calendar.DAY_OF_WEEK)).get("length"),
                        false,
                        mAccessoryPaint);

                //Add the  date in text
                canvas.drawText(Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)),
                        xCenter - accessoryOffset + 2f,
                        yCenter + 10,
                        mDayNumberPaint);

                //Draw wear battery level
                angle = ((batteryPercent-0.5f)*125f);
                batteryHand.setHandLength(accessoryCircleSize - 4f);
                batteryHand.drawHand(canvas, xCenter, yCenter - accessoryOffset, angle);

                //Draw Phone battery level
                angle = ((0.5f - mobileBatteryPercent)*125f) + 180f;
                batteryHand.setHandLength(accessoryCircleSize - 4f);
                batteryHand.drawHand(canvas, xCenter, yCenter - accessoryOffset, angle);

                //cap it off with a circle
                canvas.drawCircle(xCenter, yCenter - accessoryOffset,
                        baseMountSecondWidth, mSecondHandPaint);
            }

            // draw hours / minute / second hands
            //draw hour hand base
            canvas.drawCircle(xCenter, yCenter, baseMountWidth, mHandBasePaint);

            //calculate minutes
            angle = calendar.get(Calendar.MINUTE) / 60f * 360f + calendar.get(Calendar.SECOND) / 60f * 1f / 60f * 360f;
            if (calendar.get(Calendar.MINUTE) < 23 || calendar.get(Calendar.MINUTE) > 35) {
                minuteHand.setHandLength(yCenter - handOffsetLength - hourTickHeight);
            } else {
                handLength = Math.toRadians(angle);
                minuteHand.setHandLength((float)((yCenter - mChinSize ) / -Math.cos(handLength)) - handOffsetLength - hourTickHeight);
            }
            minuteHand.drawHand(canvas,xCenter,yCenter,angle);

            //calculate hours
            angle = calendar.get(Calendar.HOUR) / 12f * 360f + calendar.get(Calendar.MINUTE) / 60f * 1f / 12f * 360f;
            hourHand.setHandLength(yCenter * hourHandLengthPercent);
            hourHand.drawHand(canvas, xCenter, yCenter, angle);

            //calculate seconds
            if (!isInAmbientMode()) {
                angle = calendar.get(Calendar.SECOND) / 60f * 360f;

                if (calendar.get(Calendar.SECOND) < 24 ||calendar.get(Calendar.SECOND) > 36) {
                    secondHandLength = yCenter - handOffsetLength;
                } else {
                    handLength = Math.toRadians(angle);
                    secondHandLength = (float)((yCenter - mChinSize ) / -Math.cos(handLength)) - handOffsetLength;
                }

                //display seconds
                secondHand.setHandLength(secondHandLength);
                secondHand.drawHand(canvas, xCenter, yCenter, angle);

                //draw second hand base
                canvas.drawCircle(xCenter, yCenter, baseMountSecondWidth, mSecondHandPaint);
            }

            //cork it off with a hole punched through the middle
            canvas.drawCircle(xCenter, yCenter, baseMountHole, mBackgroundPaint);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                calendar.setTimeZone(TimeZone.getDefault());
                calendar.setTime(new Date());
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {

            if (!mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                MotoWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
            }

            if (!mRegisteredBatteryLevelReceiver) {
                mRegisteredBatteryLevelReceiver = true;
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                MotoWatchFace.this.registerReceiver(mBatteryLevelReceiver, filter);
            }
        }

        private void unregisterReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                mRegisteredTimeZoneReceiver = false;
                MotoWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
            }

            if (mRegisteredBatteryLevelReceiver) {
                mRegisteredBatteryLevelReceiver = false;
                MotoWatchFace.this.unregisterReceiver(mBatteryLevelReceiver);
            }

        }



        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private Map<Integer,Map<String,Float>> calculateDateLocation(){
            Map <Integer,Map<String,Float>> dayLocations = new HashMap<>(weekDays.size());
            float dayPercent = 360f/(float)weekDays.size();

            Map<Integer,Float> textSize = new HashMap<>(weekDays.size());
            float max=0f;
            for(int count=1;count<=weekDays.size();count++) {
                textSize.put(count, mAccessoryPaint.measureText(weekDays.get(count)));
                max = (max < textSize.get(count)) ? textSize.get(count) : max;
            }

            float lastEnd=0;
            for(int count=1;count<=weekDays.size();count++) {
                lastEnd+=dayPercent * textSize.get(count) / max;
            }

            float extra=(360f-lastEnd) / (float)weekDays.size();

            lastEnd=0;
            for(int count=1;count<=weekDays.size();count++) {
                dayLocations.put(count, new HashMap<String, Float>());
                dayLocations.get(count).put("start", lastEnd);
                dayLocations.get(count).put("length"
                        ,dayPercent * textSize.get(count) / max + extra);
                lastEnd+=dayLocations.get(count).get("length");
            }

            return dayLocations;
        }
    }
}