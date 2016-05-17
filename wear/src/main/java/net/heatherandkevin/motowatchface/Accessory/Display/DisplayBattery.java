package net.heatherandkevin.motowatchface.Accessory.Display;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import net.heatherandkevin.motowatchface.Accessory.AccessoryPosition;
import net.heatherandkevin.motowatchface.Accessory.AccessoryType;
import net.heatherandkevin.motowatchface.Accessory.DisplayAccessory;
import net.heatherandkevin.motowatchface.clockhand.DisplayClockHand.AccentHand;
import net.heatherandkevin.motowatchface.clockhand.ClockHand;

/**
 * Created by kmager on 5/2/16.
 */
public class DisplayBattery extends DisplayAccessory {
    private final float HAND_WIDTH = 2f;
    private final float HAND_OFFSET = 0f;
    private final float HAND_SETBACK = 6f;
    private final float BASE_MOUNT_WIDTH = 4f;
    private float handLength;
    private ClockHand batteryHand;
    private float xCenter;
    private float yCenter;
    private Paint batteryPaint;
    private RectF oval;

    private float wearBatteryAngle;
    private float mobileBatteryAngle;

    public DisplayBattery(int handPaintColor) {

        batteryPaint = new Paint();
        batteryPaint.setColor(handPaintColor);
        batteryPaint.setAntiAlias(true);
        batteryHand = new AccentHand(batteryPaint,HAND_WIDTH,HAND_OFFSET);
    }

    public void setWearBatteryLevels(float wearBatteryPercent) {
        wearBatteryAngle = ((wearBatteryPercent-0.5f)*125f);

    }

    public void setMobileBatteryPercent(float mobileBatteryPercent) {
        mobileBatteryAngle = ((0.5f - mobileBatteryPercent) * 125f) + 180f;
    }

    public float getMobileBatteryAngle() {
        return mobileBatteryAngle;
    }

    public boolean accessoryTap(int x, int y) {
        return oval.contains(x,y);
    }

    @Override
    protected void init() {
        xCenter = calculateXCenter(AccessoryPosition.TOP, faceWidth);
        yCenter = calculateYCenter(AccessoryPosition.TOP, faceHeight);

        oval = calculateRectPosition(AccessoryPosition.TOP, AccessoryType.FULL_DISPLAY, faceHeight, faceWidth);

        handLength = (ACCESSORY_CIRCLE_SIZE - HAND_SETBACK) / 320f * faceHeight;
        batteryHand.setHandLength(handLength);
    }

    @Override
    public void display(Canvas canvas) {
        batteryHand.drawHand(canvas, xCenter, yCenter, wearBatteryAngle);

        if (mobileBatteryAngle >= 0f) {
            //Draw Phone battery level
            batteryHand.drawHand(canvas, xCenter, yCenter, mobileBatteryAngle);
        }

        //cap it off with a circle
        canvas.drawCircle(xCenter, yCenter,
                BASE_MOUNT_WIDTH, batteryPaint);
    }
}
