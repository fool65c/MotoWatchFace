package net.heatherandkevin.motowatchface.clockhand;

/**
 * Created by kmager on 4/7/16.
 * used for base clock hands
 */
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Created by kmager on 3/20/16.
 * Used for base clock hands
 */
public abstract class ClockHand {
    protected Paint handPaint;
    protected float halfWidth;
    protected float handLength;

    public void setHandLength(float handLength) {
        this.handLength = handLength;
    }

    public abstract void drawHand(Canvas canvas, float xCenter, float yCenter, float angle);
}
