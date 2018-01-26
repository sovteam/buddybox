package buddybox;

import android.app.Application;

import buddybox.controller.Library;
import buddybox.controller.Player;
import buddybox.controller.Sampler;
import buddybox.impl.ModelImpl;
import buddybox.ui.ModelSingleton;

public class BuddyBoxApp extends Application {

    //TODO Check for storage permission.

    private static boolean USE_SIMULATOR = false;

    @Override
    public void onCreate() {
        super.onCreate();
        ModelSingleton.init(USE_SIMULATOR ? new ModelSim() : new ModelImpl(this));
        Player.init(this);
        Library.init();
        Sampler.init(this);
    }
}
