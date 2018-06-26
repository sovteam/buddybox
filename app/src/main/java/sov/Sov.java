package sov;

import java.io.File;

public class Sov {

    interface Listener {
        void onMessageReceived(long fromContactId, String type, Object payload);
        void onFileReceived(long fromContactId, Hash hash, File written);
    }
    void initListener(Listener listener) {}

    static Hash serve(File file) { return null; }
    void requestFile(Hash hash, long fromContactId, File toWrite) {}

    /** Launches an Android sharing Intent with a Sov link.
     * @param title Used for sharing via email, for example.
     * @param description The message body.
     * @param payload App specific payload to be serialized. Use native Android types and collections. */
    void share(String title, String description, Object payload) {}

// Mesh sharing:
//    static long[] contactIds() { return null; }
//    static void sendMessage(long toContactId, String type, Object payload, int priority, long timeoutMillis) {};

}
