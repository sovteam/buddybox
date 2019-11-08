package buddybox.io;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.Objects;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.SetHeadphonesVolume.HEADPHONES_CONNECTED;
import static buddybox.core.events.SetHeadphonesVolume.HEADPHONES_DISCONNECTED;

public class HeadsetPlugListener extends Service {

    private HeadsetPlugReceiver headsetPlugReceiver;

    public HeadsetPlugListener() {}

    public static void init(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
            for (AudioDeviceInfo device : audioDevices)
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET)
                    dispatch(HEADPHONES_CONNECTED);
        }

        Intent intent = new Intent(context, BluetoothListener.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(headsetPlugReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(headsetPlugReceiver);
    }

    class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Objects.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG)) {
                return;
            }
            boolean connectedHeadphones = (intent.getIntExtra("state", 0) == 1);
            if (connectedHeadphones)
                dispatch(HEADPHONES_CONNECTED);
            else
                dispatch(HEADPHONES_DISCONNECTED);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
