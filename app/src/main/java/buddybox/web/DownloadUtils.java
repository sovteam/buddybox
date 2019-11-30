package buddybox.web;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadUtils {

    public static byte[] download(String urlStr) {
        URL url = createUrl(urlStr);
        if (url == null)
            return null;

        byte[] ret;
        try {
            InputStream in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) != -1)
                out.write(buffer, 0, n);

            out.close();
            in.close();
            ret = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DownloadUtils", "IOException reading stream");
            return null;
        }
        return ret;
    }

    private static URL createUrl(String urlStr) {
        URL ret;
        try {
            ret = new URL(urlStr);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d("DownloadUtils", "MalformedURLException: " + urlStr);
            return null;
        }
        return ret;
    }
}
