package buddybox.core.events;

import buddybox.core.Dispatcher;

public class SetBluetoothVolume extends Dispatcher.Event {

    public final int volume;

    public SetBluetoothVolume(int volume) {
        super("SetBluetoothVolume " + volume);
        this.volume = volume;
    }
}