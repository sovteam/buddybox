package sov;

import java.io.File;

public class Sov {

    interface Listener {
        void onSovInstall();
        void onMessageReceived(long fromContactId, String type, Object payload);
        void onFileReceived(long fromContactId, Hash hash, File written);
    }
    void initListener(Listener listener) {}

    boolean isInstalled() { return false; }
    /** @param feature will complete the phrase "To _ you need to install Sov." */
    void install(String feature) {}

    static long[] contactIds() { return null; }

    static void sendMessage(long toContactId, String type, Object payload, int priority, long timeoutMillis) {};

    static Hash serve(File file) { return null; }
    void requestFile(Hash hash, long fromContactId, File toWrite) {}

}
