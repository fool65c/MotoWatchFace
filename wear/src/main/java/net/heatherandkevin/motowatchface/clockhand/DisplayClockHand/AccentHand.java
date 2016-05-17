package net.heatherandkevin.motowatchface.clockhand.DisplayClockHand;

import android.graphics.Canvas;
import android.graphics.Paint;

import net.heatherandkevin.motowatchface.clockhand.ClockHand;

/**
 * Created by kmager on 4/7/16.
 */
public class AccentHand extends ClockHand {
    private float handOffSetLength;

    public AccentHand(Paint paint, float handWidth, float handOffSetLength) {

        this.handPaint = paint;
        this.halfWidth = handWidth / 2f;
        this.handOffSetLength = handOffSetLength;
    }

    @Override
    public void drawHand(Canvas canvas, float xCenter, float yCenter, float angle) {
        canvas.save();
        canvas.rotate(angle, xCenter, yCenter);

        canvas.drawRect(xCenter - this.halfWidth,
                yCenter - this.handLength,
                xCenter + this.halfWidth,
                yCenter + this.handOffSetLength,
                this.handPaint);

        canvas.restore();
    }
}
