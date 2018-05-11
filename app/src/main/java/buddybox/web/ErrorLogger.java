package buddybox.web;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import utils.Daemon;

public class ErrorLogger {
    private static final String POST_LOG_URL = "https://sov.network/ws/log-error";

    public static void notify(Throwable throwable) {
        final Map<String,String> body = new HashMap<>();
        body.put("app", "BuddyBox");
        body.put("device", getDeviceName());
        body.put("exception", throwable.getClass().getName());
        body.put("message", throwable.getMessage());
        body.put("os", Build.VERSION.RELEASE);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        body.put("stackTrace", sw.toString());

        new Daemon("ErrorLogger.notify") { @Override public void run() {
            HttpUtils.postRequestJSON(POST_LOG_URL, body);
            Log.d("ErrorLogger", "done sending error");
        }};

    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }

        return phrase.toString();
    }
}
