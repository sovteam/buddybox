package buddybox;

import buddybox.api.Model;

public class ModelSingleton {

    private static Model INSTANCE;

    public static void init(Model instance) {
        if (INSTANCE != null) throw new IllegalStateException();
        INSTANCE = instance;
    }

    public static void dispatch(Model.Event event) {
        System.out.println(">>> Dispatch Event Type: " +  event.type);
        INSTANCE.dispatch(event);
    }

    public static void setStateListener(Model.StateListener listener) {
        INSTANCE.addStateListener(listener);
    }

}
