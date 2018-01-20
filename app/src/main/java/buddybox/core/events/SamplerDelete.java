package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SamplerDelete extends Dispatcher.Event {
    public final Song song;

    public SamplerDelete(Song song) {
        super("Sampler Delete " + song.name);
        this.song = song;
    }
}
