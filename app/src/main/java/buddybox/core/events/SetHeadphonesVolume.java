package buddybox.core.events;

import buddybox.core.Dispatcher;

public class SetHeadphonesVolume extends Dispatcher.Event {

    public final int volume;

    public SetHeadphonesVolume(int volume) {
        super ("SetHeadphonesVolume " + volume);
        this.volume = volume;
    }

    public static final Dispatcher.Event HEADPHONES_CONNECTED = new Dispatcher.Event("HeadphonesConnected");
    public static final Dispatcher.Event HEADPHONES_DISCONNECTED = new Dispatcher.Event("HeadphonesDisconnected");
}
