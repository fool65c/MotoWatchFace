package net.heatherandkevin.motowatchface.Accessory;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import net.heatherandkevin.motowatchface.clockhand.AccentHand;
import net.heatherandkevin.motowatchface.clockhand.ClockHand;

/**
 * Created by kmager on 5/2/16.
 */
public class DisplayBattery extends DisplayAccessory {
    private final float HAND_WIDTH = 2f;
    private final float HAND_OFFSET = 0f;
    private final float HAND_SETBACK = 6f;
    private final float BASE_MOUNT_WIDTH = 4f;
    private float angle;
    private float handLength;
    private ClockHand batteryHand;
    private float xCenter;
    private float yCenter;
    private Paint batteryPaint;
    private RectF oval;

    public DisplayBattery(int handPaintColor,
                          int faceHeight,
                          int faceWidth) {

        batteryPaint = new Paint();
        batteryPaint.setColor(handPaintColor);
        batteryPaint.setAntiAlias(true);

        xCenter = calculateXCenter(AccessoryPosition.TOP, faceWidth);
        yCenter = calculateYCenter(AccessoryPosition.TOP, faceHeight);

        handLength = (ACCESSORY_CIRCLE_SIZE - HAND_SETBACK) / 320f * faceHeight;

        batteryHand = new AccentHand(batteryPaint,HAND_WIDTH,HAND_OFFSET);
        batteryHand.setHandLength(handLength);

        oval = calculateRectPosition(AccessoryPosition.TOP,AccessoryType.FULL_DISPLAY,faceHeight,faceWidth);
    }

    public void display(Canvas canvas, float batteryPercent, float mobileBatteryPercent) {
        //Draw wear battery level
        angle = ((batteryPercent-0.5f)*125f);

        batteryHand.drawHand(canvas, xCenter, yCenter, angle);

        if (mobileBatteryPercent >= 0f) {
            //Draw Phone battery level
            angle = ((0.5f - mobileBatteryPercent) * 125f) + 180f;
            batteryHand.drawHand(canvas, xCenter, yCenter, angle);
        }

        //cap it off with a circle
        canvas.drawCircle(xCenter, yCenter,
                BASE_MOUNT_WIDTH, batteryPaint);
    }

    public boolean accessoryTap(int x, int y) {
        return oval.contains(x,y);
    }
}
