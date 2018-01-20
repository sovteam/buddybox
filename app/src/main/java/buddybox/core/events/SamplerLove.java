package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SamplerLove extends Dispatcher.Event {
    public final Song song;

    public SamplerLove(Song song) {
        super("Sampler Love " + song.name);
        this.song = song;
    }
}
