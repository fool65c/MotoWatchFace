package net.heatherandkevin.motowatchface.Accessory;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

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

    public DisplayWeather(int tempPaintColor,
                          Typeface font,
                          int faceHeight,
                          int faceWidth) {

        tempPaint = new Paint();
        tempPaint.setColor(tempPaintColor);
        tempPaint.setTextAlign(Paint.Align.CENTER);
        tempPaint.setTextSize(FONT_SIZE);
        tempPaint.setTypeface(font);
        tempPaint.setAntiAlias(true);

        oval = calculateRectPosition(AccessoryPosition.RIGHT,
                AccessoryType.FULL_DISPLAY,
                faceHeight,
                faceWidth);

        xCenter = calculateXCenter(AccessoryPosition.RIGHT, faceWidth);
        yCenter = calculateYCenter(AccessoryPosition.RIGHT, faceHeight);
    }

    public void display(Canvas canvas, WatchFaceWeather weather) {
        if (weather != null){
            canvas.drawBitmap(weather.getIconBitMap(),
                    xCenter - weather.getIconBitMap().getWidth() / 2f,
                    yCenter - weather.getIconBitMap().getHeight() + IMAGE_OFFSET,
                    null);

            canvas.drawText(String.format("%.1f" + (char) 0x00B0, weather.getTemp()),
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
        }
    }

    public boolean accessoryTap(int x, int y) {
        return oval.contains(x,y);
    }
}
