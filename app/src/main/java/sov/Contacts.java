package sov;

public class Contacts {

    long[] contactIds() { return null; };

    void send(long receiverId, Object payload, int priority, long timeoutMillis) {};

    void addListener(Contacts.Listener listener) {};

    public interface Listener {
        void onMessage(long senderId, Object payload);
    }
}
