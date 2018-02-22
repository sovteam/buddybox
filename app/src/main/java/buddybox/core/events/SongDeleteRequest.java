package buddybox.core.events;

import buddybox.core.Dispatcher;

public class SongDeleteRequest extends Dispatcher.Event {
    public final String songHash;

    public SongDeleteRequest(String songHash) {
        super("SongDeleteRequest " + songHash);
        this.songHash = songHash;
    }

}