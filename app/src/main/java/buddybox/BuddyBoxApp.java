package buddybox;

import android.app.Application;

import buddybox.impl.ModelImpl;
import buddybox.sim.ModelSim;

public class BuddyBoxApp extends Application {

    private static boolean USE_SIMULATOR = false;

    @Override
    public void onCreate() {
        super.onCreate();
        CoreSingleton.init(USE_SIMULATOR ? new ModelSim() : new ModelImpl(this));
    }
}
