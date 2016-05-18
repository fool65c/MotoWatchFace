package net.heatherandkevin.motowatchface.Accessory.Display;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import net.heatherandkevin.motowatchface.Accessory.AccessoryPosition;
import net.heatherandkevin.motowatchface.Accessory.AccessoryType;
import net.heatherandkevin.motowatchface.Accessory.DisplayAccessory;
import net.heatherandkevin.motowatchface.domain.WatchFaceWeather;

/**
 * Created by kmager on 5/2/16.
 */
public class DisplayWeather extends DisplayAccessory {
    private final int FONT_SIZE = 24;
    private final float IMAGE_OFFSET = 10f;
    private final float TEXT_OFFSET = 23f;
    private final float UNKNOWN_TEXT_OFFSET = 10f;
    private Paint tempPaint;
    private RectF oval;
    private float xCenter;
    private float yCenter;
    private float imageX;
    private float imageY;
    private WatchFaceWeather weather;
    private boolean initialized;
    private String tempString;

    public DisplayWeather(int tempPaintColor,
                          Typeface font) {

        tempPaint = new Paint();
        tempPaint.setColor(tempPaintColor);
        tempPaint.setTextAlign(Paint.Align.CENTER);
        tempPaint.setTextSize(FONT_SIZE);
        tempPaint.setTypeface(font);
        tempPaint.setAntiAlias(true);
        initialized = false;
    }

    public boolean accessoryTap(int x, int y) {
        return oval.contains(x,y);
    }

    public void setWeather(WatchFaceWeather weather) {
        this.weather = weather;
        tempString = String.format("%.1f" + (char) 0x00B0, weather.getTemp());
        init();
    }

    public WatchFaceWeather getWeather() {
        return weather;
    }

    @Override
    protected void init() {
        oval = calculateRectPosition(AccessoryPosition.RIGHT,
                AccessoryType.FULL_DISPLAY,
                faceHeight,
                faceWidth);

        xCenter = calculateXCenter(AccessoryPosition.RIGHT, faceWidth);
        yCenter = calculateYCenter(AccessoryPosition.RIGHT, faceHeight);

        if (weather != null) {
            imageX = xCenter - weather.getIconBitMap().getWidth() / 2f;
            imageY = yCenter - weather.getIconBitMap().getHeight() + IMAGE_OFFSET;
            initialized=true;
        }

    }

    @Override
    public void display(Canvas canvas) {
        if (weather != null && initialized && weather.getIconBitMap() != null){
            canvas.drawBitmap(weather.getIconBitMap(),
                    imageX,
                    imageY,
                    null);

            canvas.drawText(tempString,
                    xCenter,
                    yCenter + TEXT_OFFSET,
                    tempPaint);

            canvas.drawArc(oval,
                    -90,
                    -360f * weather.percentLeftInStage(System.currentTimeMillis() / 1000),
                    false,
                    weather.getPercentPaint(System.currentTimeMillis() / 1000));
        } else {
            canvas.drawText("--",
                    xCenter,
                    yCenter + UNKNOWN_TEXT_OFFSET,
                    tempPaint);
            init();
        }
    }
}
