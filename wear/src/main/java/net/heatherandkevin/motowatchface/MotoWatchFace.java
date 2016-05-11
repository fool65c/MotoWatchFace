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
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
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

import net.heatherandkevin.motowatchface.Accessory.DisplayBattery;
import net.heatherandkevin.motowatchface.Accessory.DisplayCalendar;
import net.heatherandkevin.motowatchface.Accessory.DisplayWeather;
import net.heatherandkevin.motowatchface.ClockFace.ClockTick;
import net.heatherandkevin.motowatchface.clockhand.AccentHand;
import net.heatherandkevin.motowatchface.clockhand.ClockHand;
import net.heatherandkevin.motowatchface.clockhand.MainHand;
import net.heatherandkevin.motowatchface.domain.WatchFaceWeather;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
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
    float mobileBatteryPercent = -1f;

    private static final String WEATHER_URI = "/WEATHER_STATS";
    private WatchFaceWeather weather;


    @Override
    public void onConnected(Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

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
                    mobileBatteryPercent = map.getFloat("BatteryLevel");
                    break;
                case WEATHER_URI:
                    weather = new WatchFaceWeather(map, getResources());
                    break;
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

        //Setting up paint colors
        Paint mBackgroundPaint;

        Paint mHandPaint;
        Paint mHandBasePaint;
        Paint mHandTipPaint;
        Paint mSecondHandPaint;

        /**
         * TESTIG FOR STUFF
         */
        DisplayCalendar displayCalendar;
        DisplayWeather displayWeather;
        DisplayBattery displayBattery;

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

            Drawable backgroundDrawable = resources.getDrawable(R.drawable.watchface2, null);
            Drawable backgroundAmbientDrawable = resources.getDrawable(R.drawable.watchfaceambient2, null);
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

            hourHand = new MainHand(mHandPaint, mHandTipPaint, hourHandWidth);
            minuteHand = new MainHand(mHandPaint, mHandTipPaint, minuteHandWidth);
            secondHand = new AccentHand(mSecondHandPaint, secondHandWidth, handOffsetLength * 2f);

            calendar = new GregorianCalendar(TimeZone.getDefault());
        }

        @Override
        public void onTapCommand(
                @TapType int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case WatchFaceService.TAP_TYPE_TAP:
                case WatchFaceService.TAP_TYPE_TOUCH:
                    Log.i("KMAGER", "TAP_TYPE_TOUCH");
                    if ( displayWeather.accessoryTap(x,y) || displayBattery.accessoryTap(x,y)) {
                        Log.i("KMAGER", "WE TAPPED ONE OF THE ACCESSORIES");
                        if (mobileBatteryPercent < 0f || weather == null) {
                            Log.i("KMAGER","ACCESSORY TAPPED AND NEEDS HELP");
                        }
                    }
                    break;

                case WatchFaceService.TAP_TYPE_TOUCH_CANCEL:
                    Log.i("KMAGER", "TAP_TYPE_TOUCH_CANCEL");
                    break;

                default:
                    Log.i("KMAGER", "DEFAULT");
                    super.onTapCommand(tapType, x, y, eventTime);
                    break;
            }
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

            if (!isInAmbientMode()) {
                if (displayCalendar == null) {
                    displayCalendar = new DisplayCalendar(getResources().getColor(R.color.accessoryColor),
                            getResources().getColor(R.color.dayColor),
                            Typeface.createFromAsset(getAssets(), "fonts/AC.ttf"),
                            faceHeight,
                            faceWidth);
                }
                displayCalendar.display(canvas, calendar.get(Calendar.DAY_OF_WEEK), calendar.get(Calendar.DAY_OF_MONTH));

                if (displayWeather == null) {
                    displayWeather = new DisplayWeather(getResources().getColor(R.color.dayColor),
                            Typeface.createFromAsset(getAssets(), "fonts/AC.ttf"),
                            faceHeight,
                            faceWidth);
                }
                displayWeather.display(canvas, weather);

                if (displayBattery == null) {
                    displayBattery = new DisplayBattery(getResources().getColor(R.color.secondHandColor),
                            faceHeight,
                            faceWidth);
                }
                displayBattery.display(canvas, batteryPercent, mobileBatteryPercent);
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
    }
}