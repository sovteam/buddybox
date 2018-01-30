package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playlist;

public class Play extends Dispatcher.Event {
    public final int songIndex;
    public final boolean isShuffle;
    public final Playlist playlist;

    public Play(Playlist playlist, int songIndex, boolean isShuffle) {
        super ("Play");
        this.playlist = playlist;
        this.songIndex = songIndex;
        this.isShuffle = isShuffle;
    }

    public Play(Playlist playlist, int songIndex) {
        this(playlist, songIndex, false);
    }

    public Play(Playlist playlist, boolean isShuffle) {
        this(playlist, 0, isShuffle);
    }

    public static final Dispatcher.Event PLAY_PAUSE_CURRENT = new Dispatcher.Event("PlayPauseCurrent");
    public static final Dispatcher.Event SKIP_NEXT = new Dispatcher.Event("SkipNext");
    public static final Dispatcher.Event SKIP_PREVIOUS = new Dispatcher.Event("SkipPrevious");
    public static final Dispatcher.Event REPEAT_SONG = new Dispatcher.Event("RepeatSong");
    public static final Dispatcher.Event REPEAT_ALL = new Dispatcher.Event("RepeatAll");


    public static final Dispatcher.Event FINISHED_PLAYING = new Dispatcher.Event("FinishedPlaying");

}