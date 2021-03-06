package buddybox.ui.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static buddybox.ui.ModelProxy.dispatch;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;

public class NotificationPlayPauseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println(">>> Notification Play/Pause button");
        dispatch(PLAY_PAUSE_CURRENT);
    }
}
