package net.heatherandkevin.motowatchface.clockhand.DisplayClockHand;

import android.graphics.Canvas;
import android.graphics.Paint;

import net.heatherandkevin.motowatchface.clockhand.ClockHand;

/**
 * Created by kmager on 4/7/16.
 * main clock hands
 */
public class MainHand extends ClockHand {
    private Paint handTipPaint;
    private Paint handPaintOpening;
    private Float borderWidth;
    private Float tipOffset;
    private Float handOpeningPercent;

    public MainHand(Paint paint, Paint handTipPaint, float handWidth) {
        this.handPaint = paint;
        this.handTipPaint = handTipPaint;
        this.halfWidth = handWidth / 2f;
        this.borderWidth = 2f;
        this.tipOffset = 5f;
        this.handOpeningPercent = 0.45f;

        this.handPaintOpening = new Paint(handPaint);
        this.handPaintOpening.setStyle(Paint.Style.STROKE);
        this.handPaintOpening.setStrokeWidth(this.borderWidth);
        this.handPaintOpening.setAntiAlias(true);

    }

    public void drawHand(Canvas canvas, float xCenter, float yCenter, float angle) {
        canvas.save();
        canvas.rotate(angle, xCenter, yCenter);

        canvas.drawRect(xCenter - this.halfWidth,
                yCenter - this.handLength * this.handOpeningPercent,
                xCenter + this.halfWidth,
                yCenter,
                this.handPaint);

        canvas.drawRect(xCenter - this.halfWidth + this.borderWidth / 2f,
                yCenter - this.handLength + this.tipOffset + this.borderWidth / 2f,
                xCenter + this.halfWidth - this.borderWidth / 2f,
                yCenter - this.handLength * this.handOpeningPercent,
                this.handPaintOpening);

        canvas.drawRect(xCenter - this.halfWidth,
                yCenter - this.handLength,
                xCenter + this.halfWidth,
                yCenter - this.handLength + this.tipOffset,
                this.handTipPaint);

        canvas.restore();
    }
}
