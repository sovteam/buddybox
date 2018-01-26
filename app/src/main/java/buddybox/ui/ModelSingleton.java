package buddybox.ui;

import buddybox.core.Dispatcher;
import buddybox.core.Model;

public class ModelSingleton {

    private static Model INSTANCE;

    public static void init(Model instance) {
        if (INSTANCE != null) throw new IllegalStateException();
        INSTANCE = instance;
    }

    public static void dispatch(Dispatcher.Event event) {
        System.out.println(">>> Dispatch Event Type: " +  event.type);
        Dispatcher.dispatch(event);
    }

    public static void addStateListener(Model.StateListener listener) {
        INSTANCE.addStateListener(listener);
    }

}
