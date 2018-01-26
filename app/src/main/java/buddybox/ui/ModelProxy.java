package buddybox.ui;

import buddybox.core.Dispatcher;
import buddybox.core.IModel;

public class ModelProxy {

    private static IModel INSTANCE;

    public static void init(IModel instance) {
        if (INSTANCE != null) throw new IllegalStateException();
        INSTANCE = instance;
    }

    public static void dispatch(Dispatcher.Event event) {
        System.out.println(">>> Dispatch Event Type: " +  event.type);
        Dispatcher.dispatch(event);
    }

    public static void addStateListener(IModel.StateListener listener) {
        INSTANCE.addStateListener(listener);
    }

}