package buddybox.core;

import java.util.ArrayList;
import java.util.List;

public class Dispatcher {

    private static List<Listener> listeners = new ArrayList<>();

    public static class Event {
        public final String type;

        public Event(String type) {
            this.type = type;
        }
    }

    public interface Listener {
        void onEvent(Event event);
    }

    public static void addListener(Listener listener) {
        listeners.add(listener);
    }

    public static void reset() {
        listeners = new ArrayList<>();
    }

    public static void dispatch(Event event) {
        for (Listener listener : listeners)
            listener.onEvent(event);
    }
}
