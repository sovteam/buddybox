package com.adalbertosoares.buddybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationPlayPauseReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println(">>> Notification Play/Pause button");
    }
}
