package buddybox;

import buddybox.api.Core;

public class CoreSingleton {

    private static Core INSTANCE;

    public static void init(Core instance) {
        if (INSTANCE != null) throw new IllegalStateException();
        INSTANCE = instance;
    }

    public static void dispatch(Core.Event event) {
        System.out.println(">>> Dispatch Event Type: " +  event.type);
        INSTANCE.dispatch(event);
    }

    public static void setStateListener(Core.StateListener listener) {
        INSTANCE.setStateListener(listener);
    }

}
