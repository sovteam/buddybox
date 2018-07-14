package buddybox.ui;

import buddybox.core.Dispatcher;
import buddybox.core.IModel;

public class ModelProxy {

    private static IModel INSTANCE;

    static void init(IModel instance) {
        if (isInitialized()) throw new IllegalStateException();
        INSTANCE = instance;
    }

    public static void dispatch(Dispatcher.Event event) {
        System.out.println(">>> Dispatch Event Type: " +  event.type);
        Dispatcher.dispatch(event);
    }

    public static void addStateListener(IModel.StateListener listener) {
        if (isInitialized())
            INSTANCE.addStateListener(listener);
    }

    public static void removeStateListener(IModel.StateListener listener) {
        INSTANCE.removeStateListener(listener);
    }

    public static boolean isInitialized() {
        return INSTANCE != null;
    }
}
