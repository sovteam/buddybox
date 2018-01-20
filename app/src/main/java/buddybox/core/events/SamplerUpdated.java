package buddybox.core.events;

import java.util.List;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SamplerUpdated extends Dispatcher.Event {

    public final List<Song> samples;

    public SamplerUpdated(List<Song> samples) {
        super("SamplerUpdated: " + samples.size() + " samples");
        this.samples = samples;
    }
}
