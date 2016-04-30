package net.heatherandkevin.motowatchface.domain;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.android.gms.wearable.DataMap;

import net.heatherandkevin.motowatchface.R;

/**
 * Created by kmager on 4/29/16.
 */
public class WatchFaceWeather extends Weather {

    private Bitmap iconBitMap;
    private Paint percentPaint;
    private int nightPaintColor;
    private int dayPaintColor;

    public WatchFaceWeather(DataMap dataMap, Resources resources) {
        icon = dataMap.getString("icon");
        iconBitMap = getDrawableIcon(resources);
        temp = dataMap.getFloat("temp");
        tempHigh = dataMap.getFloat("high");
        tempLow = dataMap.getFloat("low");
        sunrise = dataMap.getLong("sunrise");
        sunset = dataMap.getLong("sunset");

        nightPaintColor = resources.getColor(R.color.nightPercentColor);
        dayPaintColor = resources.getColor(R.color.dayLightPercentColor);

        percentPaint = new Paint();
        percentPaint.setStrokeWidth(3);
        percentPaint.setStyle(Paint.Style.STROKE);
        percentPaint.setAntiAlias(true);
    }

    private Bitmap getDrawableIcon(Resources resources) {
        Drawable iconDrawable = resources.getDrawable(resources.getIdentifier("weathericon" + icon, "drawable", "net.heatherandkevin.motowatchface"));
        return ((BitmapDrawable) iconDrawable).getBitmap();

    }
    
    public Bitmap getIconBitMap() {
        return iconBitMap;
    }

    private boolean isDayTime(long currentTime) {
        return currentTime > sunrise && currentTime < sunset;
    }

    public float percentLeftInStage(long currentTime) {
        float percentLeft;
        if (isDayTime(currentTime)) {
            percentLeft = ( sunset - (float) currentTime ) / (sunset - sunrise);
        } else {
            percentLeft = ( sunrise - (float) currentTime ) /  ( 86400f + sunrise - sunset );
        }

        return percentLeft;
    }

    public Paint getPercentPaint(long currentTime) {
        if (isDayTime(currentTime)) {
            percentPaint.setColor(dayPaintColor);
        } else {
            percentPaint.setColor(nightPaintColor);
        }

        return percentPaint;
    }
}
