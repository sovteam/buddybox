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
import android.support.annotation.Nullable;
import android.view.KeyEvent;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Bluetooth.BLUETOOTH_CONNECT;
import static buddybox.core.events.Bluetooth.BLUETOOTH_DISCONNECT;

public class BluetoothDetectService extends Service {

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        System.out.println(">>> Broadcast received!");
        final String action = intent.getAction();
        // BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (action == null)
            return;

        switch(action) {
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                System.out.println("@@@ Bluetooth connected");
                dispatch(BLUETOOTH_CONNECT);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                System.out.println("@@@ Bluetooth disconnected");
                dispatch(BLUETOOTH_DISCONNECT);
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    System.out.println("@@@ Bluetooth OFF");
                    dispatch(BLUETOOTH_DISCONNECT);
                }
                break;
            default:
                break;
        }
        }
    };

    public BluetoothDetectService() {}

    public static void init(Context context) {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        // TODO test start app with connected device (headset/speaker/cellphone)
        if (bluetooth != null
                && bluetooth.isEnabled()
                && bluetooth.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED)
            dispatch(BLUETOOTH_CONNECT);

        Intent intent = new Intent(context, BluetoothDetectService.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.EXTRA_CLASS);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
