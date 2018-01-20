package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playlist;

public class Play extends Dispatcher.Event {
    public final int songIndex;
    public final Playlist playlist;

    public Play(Playlist playlist, int songIndex) {
        super ("Play");
        this.playlist = playlist;
        this.songIndex = songIndex;
    }

    public static final Dispatcher.Event PLAY_PAUSE_CURRENT = new Dispatcher.Event("PlayPauseCurrent");
    public static final Dispatcher.Event SKIP_NEXT = new Dispatcher.Event("SkipNext");
    public static final Dispatcher.Event SKIP_PREVIOUS = new Dispatcher.Event("SkipPrevious");

    public static final Dispatcher.Event FINISHED_PLAYING = new Dispatcher.Event("FinishedPlaying");

}