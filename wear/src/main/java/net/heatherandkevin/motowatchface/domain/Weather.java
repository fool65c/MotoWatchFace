package net.heatherandkevin.motowatchface.domain;

import com.google.android.gms.wearable.DataMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kmager on 4/24/16.
 */
public class Weather {
    protected String icon;
    protected float temp;
    protected float tempHigh;
    protected float tempLow;
    protected long sunrise;
    protected long sunset;

    public Weather() {}

    public Weather(JSONObject jsonObject) throws JSONException {
        JSONObject weatherObject =  (JSONObject) jsonObject.getJSONArray("weather").get(0);
        icon = weatherObject.getString("icon");
        temp = (float) jsonObject.getJSONObject("main").getDouble("temp");
        tempHigh = (float) jsonObject.getJSONObject("main").getDouble("temp_max");
        tempLow = (float) jsonObject.getJSONObject("main").getDouble("temp_min");
        sunrise = jsonObject.getJSONObject("sys").getInt("sunrise");
        sunset = jsonObject.getJSONObject("sys").getInt("sunset");
    }

    public String getIcon() {
        return icon;
    }

    public float getTemp() { return temp; }

    public float getTempHigh() {
        return tempHigh;
    }

    public float getTempLow() { return tempLow; }

    public long getSunrise() {
        return sunrise;
    }

    public long getSunset() {
        return sunset;
    }
}
