package buddybox.web;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import static java.nio.charset.StandardCharsets.UTF_8;

public class HttpUtils {

    public static JSONObject getJson(String urlString) throws IOException {
        return getJson(new URL(urlString));
    }

    public static JSONObject getJson(URL url) throws IOException {
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        try {
            http.setRequestProperty("User-Agent", "BuddyBox App");

            if (http.getResponseCode() != 200)
                throw new IOException("Response code not 200: " + http.getResponseCode() + " URL: " + url);

            return readJSON(http.getInputStream());
        } finally {
            http.disconnect();
        }
    }

    private static JSONObject readJSON(InputStream inputStream) throws IOException {
        try {
            return new JSONObject(readString(inputStream));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public static void postJson(String urlString, Map<String, String> body) throws IOException {

        HttpsURLConnection https = (HttpsURLConnection)new URL(urlString).openConnection();
        try {
            https.setConnectTimeout(5000);
            https.setRequestMethod("POST");
            https.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            byte[] bytesOut = new JSONObject(body).toString().getBytes(UTF_8);
            try (OutputStream os = https.getOutputStream()) {
                os.write(bytesOut);
            }

            Log.i("HttpUtils", ">>>> postJson returns: " + readString(https.getInputStream()));
        } finally {
            https.disconnect();
        }
    }

    private static String readString(InputStream inputStream) throws IOException {
        StringBuilder ret = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
        String line;
        while ((line = reader.readLine()) != null)
            ret.append(line).append("\n");

        return ret.toString();
    }
}


