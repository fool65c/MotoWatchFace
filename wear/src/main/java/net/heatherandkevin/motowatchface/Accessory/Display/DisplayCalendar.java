package net.heatherandkevin.motowatchface.Accessory.Display;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import net.heatherandkevin.motowatchface.Accessory.AccessoryPosition;
import net.heatherandkevin.motowatchface.Accessory.AccessoryType;
import net.heatherandkevin.motowatchface.Accessory.DisplayAccessory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kmager on 5/1/16.
 * displays the calendar
 */
public class DisplayCalendar extends DisplayAccessory {
    private static final int FONT_SIZE = 34;
    private Map <Integer,Map<String,Float>> dayLocations;
    private Paint daySelectionPaint;
    private Paint dayNumberPaint;
    private RectF oval;
    private float xCenter, yCenter;

    //used for display
    private int dayOfMonth;
    private int dayOfWeek;
    private boolean dimensionsInitialized;
    private boolean calendarInitialized;

    public DisplayCalendar(int daySelectionPaintColor,
                           int dayNumberPaintColor,
                           Typeface font) {
        daySelectionPaint = new Paint();
        daySelectionPaint.setColor(daySelectionPaintColor);
        daySelectionPaint.setStrokeWidth(STROKE);
        daySelectionPaint.setStyle(Paint.Style.STROKE);
        daySelectionPaint.setAntiAlias(true);

        dayNumberPaint = new Paint();
        dayNumberPaint.setColor(dayNumberPaintColor);
        dayNumberPaint.setTextAlign(Paint.Align.CENTER);
        dayNumberPaint.setTextSize(FONT_SIZE);
        dayNumberPaint.setTypeface(font);
        dayNumberPaint.setAntiAlias(true);

        dimensionsInitialized = false;
        calendarInitialized = false;

        calculateDateLocation();
    }

    public void setDisplayData(int dayOfWeek, int dayOfMonth) {
        if (this.dayOfWeek != dayOfWeek || this.dayOfMonth != dayOfMonth) {
            this.dayOfWeek = dayOfWeek;
            this.dayOfMonth = dayOfMonth;
            this.calendarInitialized = true;
        }
    }

    @Override
    protected void init() {
        oval = calculateRectPosition(AccessoryPosition.LEFT,
                AccessoryType.INNER_DISPLAY,
                faceHeight,
                faceWidth);

        xCenter = calculateXCenter(AccessoryPosition.LEFT, faceWidth);
        yCenter = calculateYCenter(AccessoryPosition.LEFT, faceHeight) + 12f;

        dimensionsInitialized = true;
    }

    public void display(Canvas canvas) {
        if (dimensionsInitialized && calendarInitialized) {
            //DRAW THE DAY
            canvas.drawArc(oval,
                    dayLocations.get(dayOfWeek).get("start"),
                    dayLocations.get(dayOfWeek).get("length"),
                    false,
                    daySelectionPaint);

            //Add the  date in text
            canvas.drawText(Integer.toString(dayOfMonth),
                    xCenter,
                    yCenter,
                    dayNumberPaint);
        }
    }

    private void calculateDateLocation() {
        Map<Integer,String> weekDays = new HashMap<>(7);
        weekDays.put(7, "SAT");
        weekDays.put(6, "FRI");
        weekDays.put(5, "THU");
        weekDays.put(4, "WED");
        weekDays.put(3, "TUE");
        weekDays.put(2, "MON");
        weekDays.put(1, "SUN");

        dayLocations = new HashMap<>(weekDays.size());
        float dayPercent = 360f/(float)weekDays.size();

        Map<Integer,Float> textSize = new HashMap<>(weekDays.size());
        float max=0f;
        for(int count=1;count<=weekDays.size();count++) {
            textSize.put(count, daySelectionPaint.measureText(weekDays.get(count)));
            max = (max < textSize.get(count)) ? textSize.get(count) : max;
        }

        float lastEnd=0;
        for(int count=1;count<=weekDays.size();count++) {
            lastEnd+=dayPercent * textSize.get(count) / max;
        }

        float extra=(360f-lastEnd) / (float)weekDays.size();

        lastEnd=0;
        for(int count=1;count<=weekDays.size();count++) {
            dayLocations.put(count, new HashMap<String, Float>());
            dayLocations.get(count).put("start", lastEnd);
            dayLocations.get(count).put("length"
                    ,dayPercent * textSize.get(count) / max + extra);
            lastEnd+=dayLocations.get(count).get("length");
        }
    }
}
