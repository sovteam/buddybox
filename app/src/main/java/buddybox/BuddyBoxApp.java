package buddybox;

import android.app.Application;

import buddybox.impl.CoreImpl;
import buddybox.sim.CoreSim;

public class BuddyBoxApp extends Application {

    private static boolean USE_SIMULATOR = true;

    @Override
    public void onCreate() {
        super.onCreate();
        CoreSingleton.init(USE_SIMULATOR ? new CoreSim() : new CoreImpl(this));
    }
}
