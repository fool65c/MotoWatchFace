package net.heatherandkevin.motowatchface.Accessory;

import android.graphics.Canvas;
import android.graphics.RectF;

/**
 * Created by kmager on 5/1/16.
 */
public abstract class DisplayAccessory {
    private final float OUTER_OFFSET = 30.0f;
    private final float INNER_OFFSET = 8.0f;
    protected final float ACCESSORY_CIRCLE_SIZE = 42f;
    protected final float STROKE = 3f;

    protected int faceHeight;
    protected int faceWidth;

    protected abstract void init();
    public abstract void display(Canvas canvas);

    public void setFaceDimensions(int faceHeight, int faceWidth) {
        if (this.faceHeight != faceHeight || this.faceWidth != faceWidth) {
            this.faceHeight = faceHeight;
            this.faceWidth = faceWidth;
            init();
        }
    }


    protected float calculateXCenter(AccessoryPosition position, int faceWidth) {
        float xCenter;
        float outerOffset = OUTER_OFFSET / 320f * faceWidth;
        float innerOffset = INNER_OFFSET / 320f * faceWidth;
        switch (position) {
            case LEFT:
                xCenter =  faceWidth / 2f - (faceWidth / 2f - outerOffset + innerOffset) /2f;
                break;
            case RIGHT:
                xCenter =  faceWidth / 2f + (faceWidth / 2f - outerOffset + innerOffset) /2f;
                break;
            case TOP:
            case BOTTOM:
            default:
                xCenter = faceWidth / 2f;
                break;
        }

        return xCenter;

    }

    protected float calculateYCenter(AccessoryPosition position, int faceHeight) {
        float yCenter;
        float outerOffset = OUTER_OFFSET / 320f * faceHeight;
        switch (position) {
            case TOP:
                yCenter =  faceHeight / 2f - (faceHeight / 2f - outerOffset + INNER_OFFSET) /2f;
                break;
            case BOTTOM:
                yCenter =  faceHeight / 2f + (faceHeight / 2f - outerOffset + INNER_OFFSET) /2f;
                break;
            case LEFT:
            case RIGHT:
            default:
                yCenter = faceHeight / 2f;
                break;
        }

        return yCenter;

    }

    protected RectF calculateRectPosition(AccessoryPosition position,
                                           AccessoryType type,
                                           int faceHeight,
                                           int faceWidth) {

        float xCenter = (float) faceWidth /2f;
        float yCenter = (float) faceHeight /2f;
        float accessoryOffset, accessoryCircleSize;
        float outerOffset = OUTER_OFFSET / 320f * faceHeight;

        switch (type) {
            case INNER_DISPLAY:
                accessoryCircleSize = 31f / 320f * faceWidth;
                break;
            case GAUGE:
            case FULL_DISPLAY:
            default:
                accessoryCircleSize = ACCESSORY_CIRCLE_SIZE / 320f * faceHeight;
                break;
        }

        float left,top,right,bottom;
        switch (position) {
            case LEFT:
                accessoryOffset =  (xCenter - outerOffset + INNER_OFFSET) /2f;
                left = xCenter - accessoryOffset + ((float) Math.floor(accessoryCircleSize)) - STROKE;
                top = yCenter  - accessoryCircleSize + STROKE;
                right = xCenter - accessoryOffset - accessoryCircleSize + STROKE;
                bottom = yCenter + ((float) Math.ceil(accessoryCircleSize)) - STROKE;
                break;
            case RIGHT:
                accessoryOffset =  (xCenter - outerOffset + INNER_OFFSET) /2f;
                left = xCenter + accessoryOffset + accessoryCircleSize - STROKE;
                top = yCenter - accessoryCircleSize + STROKE;
                right = xCenter + accessoryOffset - accessoryCircleSize + STROKE;
                bottom = yCenter + accessoryCircleSize - STROKE;
                break;
            case TOP:
                accessoryOffset =  (yCenter - outerOffset + INNER_OFFSET) /2f;
                left = xCenter + accessoryCircleSize - STROKE;
                top = yCenter  - accessoryOffset - accessoryCircleSize + STROKE;
                right = xCenter - accessoryCircleSize + STROKE;
                bottom = yCenter - accessoryOffset + accessoryCircleSize - STROKE;
                break;
            case BOTTOM:
            default:
                accessoryOffset =  (yCenter - outerOffset + INNER_OFFSET) /2f;
                left = xCenter + accessoryCircleSize - STROKE;
                top = yCenter  + accessoryOffset - accessoryCircleSize + STROKE;
                right = xCenter - accessoryCircleSize + STROKE;
                bottom = yCenter + accessoryOffset + accessoryCircleSize - STROKE;
                break;
        }

        return new RectF(right,top,left,bottom);
    }
}
