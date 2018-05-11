package buddybox.web;

import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class HttpUtils {

    public static JSONObject getHttpResponse(String urlString) {
        // build URL
        URL url = getUrl(urlString);
        if (url == null)
            return null;

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

    @Nullable
    private static URL getUrl(String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
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

    public static void postRequestJSON(String urlString, Map<String, String> body) {
        URL url = getUrl(urlString);
        if (url == null)
            return;

        try {
            URLConnection con = url.openConnection();
            HttpsURLConnection https = (HttpsURLConnection)con;
            https.setConnectTimeout(5000);
            https.setRequestMethod("POST");
            https.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            JSONObject json = new JSONObject(body);
            OutputStream os = https.getOutputStream();
            os.write(json.toString().getBytes("UTF-8"));
            os.close();

            // get return
            StringBuilder ret = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(https.getInputStream(),"utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                ret.append(line).append("\n");
            }
            br.close();
            Log.i("HttpUtils", ">>>> postRequestJSON returns: " + ret);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


