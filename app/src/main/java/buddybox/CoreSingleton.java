package buddybox;

import java.util.HashMap;
import java.util.Map;

import buddybox.api.Core;
import buddybox.sim.CoreSim;

public class CoreSingleton {

    private static final Core INSTANCE = new CoreSim(); // switching between sims and real impl is done here

    public static void dispatch(Core.Event event) {
        INSTANCE.dispatch(event);
    }

    public static void setStateListener(Core.StateListener listener) {
        INSTANCE.setStateListener(listener);
    }

}
