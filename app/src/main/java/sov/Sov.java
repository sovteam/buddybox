package sov;

import java.io.File;

public class Sov {

    class Download {
        long fromContactId;
        File file;
    }

    interface DownloadListener {
        void onStarted(Download download);
        void onProgress(Download download, double percentage);
        void onCompleted(Download download);
        void onAborted(Download download);
    }
    /** Called typically from Application.onCreate() */
    void initDownloadListener(DownloadListener listener) {}

    void abort(Download download) {};

    /** Launches an Android sharing Intent with a Sov link.
     * @param subject Used for sharing via email, for example.
     * @param description The message body. */
    void shareFile(String subject, String description, File toSend) {}


// Mesh sharing:
//    static long[] contactIds() { return null; }
//    static void sendMessage(long toContactId, String type, Object payload, int priority, long timeoutMillis) {};
//    static Hash serve(File file) { return null; }
//    void requestFile(Hash hash, long fromContactId, File toWrite) {}

}
