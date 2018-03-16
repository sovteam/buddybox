package buddybox.web;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpUtils {

    public static JSONObject getHttpResponse(String urlString) {
        // build URL
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        // open connection
        int responseCode;
        HttpURLConnection urlConnection;
        InputStream result;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Buddy Box App");
            responseCode = urlConnection.getResponseCode();
            result = urlConnection.getInputStream();
            // check response code
            if (responseCode != 200) {
                Log.d("MediaInfoRetriever", "RESPONSE CODE " + responseCode);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject ret = buildJSON(result);
        urlConnection.disconnect();
        return ret;
    }

    private static JSONObject buildJSON(InputStream inputStream) {
        StringBuilder builder;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject ret;
        try {
            ret = new JSONObject(builder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return ret;
    }
}


