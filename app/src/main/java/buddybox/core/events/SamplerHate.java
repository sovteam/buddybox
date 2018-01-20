package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SamplerHate extends Dispatcher.Event {
    public final Song song;

    public SamplerHate(Song song) {
        super("Sampler Hate " + song.name);
        this.song = song;
    }
}
