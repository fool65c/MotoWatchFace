package net.heatherandkevin.motowatchface.service;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by kmager on 4/25/16.
 */
public class WeatherHttpClient {

    private static String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?";
    private static String API_KEY = "ce6f7a2036b24264b5718a1ce4daf875";

    public JSONObject getWeatherData(double lat, double lon) {
        HttpURLConnection con = null ;
        InputStream is = null;

        String uri = String.format("lat=%03f&lon=%03f&units=Imperial&appid=%s", lat, lon, API_KEY);

        try {
            con = (HttpURLConnection) ( new URL(BASE_URL + uri)).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

// Let's read the response
            StringBuffer buffer = new StringBuffer();
            is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ( (line = br.readLine()) != null )
                buffer.append(line + "rn");

            is.close();
            con.disconnect();
            return new JSONObject(buffer.toString());
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
        finally {
            try { is.close(); } catch(Throwable t) {}
            try { con.disconnect(); } catch(Throwable t) {}
        }

        return null;

    }
}
