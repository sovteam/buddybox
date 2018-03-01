package sov;

import java.io.File;

public class Sov {

    static long[] contactIds() { return null; };

    static void sendMessage(long toContactId, String type, Object payload, int priority, long timeoutMillis) {};

    static void addMessageListener(MessageListener listener) {};

    public interface MessageListener {
        void onMessageReceived(long fromContactId, String type, Object payload);
    }

    //////////////////Files

    static Hash serve(File file) {}

    void requestFile(Hash hash, long fromContactId, File toWrite) {}

    static void addFileListener(FileListener listener) {};

    public interface FileListener {
        void onFileReceived(Hash hash, long fromContactId, File written);
    }


}
