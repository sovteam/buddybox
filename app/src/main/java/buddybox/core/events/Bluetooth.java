package buddybox.core.events;

import buddybox.core.Dispatcher;

public class Bluetooth {
    public static final Dispatcher.Event BLUETOOTH_CONNECT = new Dispatcher.Event("BluetoothConnect");
    public static final Dispatcher.Event BLUETOOTH_DISCONNECT = new Dispatcher.Event("BluetoothDisconnect");
}
