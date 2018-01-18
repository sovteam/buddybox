package buddybox.ui.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static buddybox.ModelSingleton.dispatch;
import static buddybox.api.Play.PLAY_PAUSE_CURRENT;

public class NotificationPlayPauseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println(">>> Notification Play/Pause button");
        dispatch(PLAY_PAUSE_CURRENT);
    }
}
