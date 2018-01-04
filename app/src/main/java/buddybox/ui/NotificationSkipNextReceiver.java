package buddybox.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static buddybox.CoreSingleton.dispatch;
import static buddybox.api.Play.SKIP_NEXT;

public class NotificationSkipNextReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        dispatch(SKIP_NEXT);
    }
}
