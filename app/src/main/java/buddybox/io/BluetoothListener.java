package buddybox.io;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import androidx.annotation.Nullable;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Bluetooth.BLUETOOTH_CONNECT;
import static buddybox.core.events.Bluetooth.BLUETOOTH_DISCONNECT;

public class BluetoothListener extends Service {

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action == null)
            return;

        switch(action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                dispatch(BLUETOOTH_CONNECT);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                dispatch(BLUETOOTH_DISCONNECT);
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF)
                    dispatch(BLUETOOTH_DISCONNECT);
                break;
            default:
                break;
        }
        }
    };

    public BluetoothListener() {}

    public static void init(Context context) {
        Intent intent = new Intent(context, BluetoothListener.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null
                && bluetooth.isEnabled()
                && bluetooth.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED)
            dispatch(BLUETOOTH_CONNECT);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.EXTRA_CLASS);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
