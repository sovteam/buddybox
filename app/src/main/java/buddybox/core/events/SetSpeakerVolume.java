package buddybox.core.events;

import buddybox.core.Dispatcher;

public class SetSpeakerVolume extends Dispatcher.Event {

    public final int volume;

    public SetSpeakerVolume(int volume) {
        super("SetSpeakerVolume " + volume);
        this.volume = volume;
    }
}