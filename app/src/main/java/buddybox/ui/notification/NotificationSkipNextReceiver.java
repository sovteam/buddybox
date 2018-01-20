package buddybox.ui.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static buddybox.ModelSingleton.dispatch;
import static buddybox.core.events.Play.SKIP_NEXT;

public class NotificationSkipNextReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        dispatch(SKIP_NEXT);
    }
}
