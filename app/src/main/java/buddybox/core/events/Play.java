package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playable;
import buddybox.core.Playlist;

public class Play extends Dispatcher.Event {
    public final Playable playable;

    public Play(Playable playable) {
        super ("Play");
        this.playable = playable;
    }

    public static final Dispatcher.Event REPEAT = new Dispatcher.Event("Repeat");
    public static final Dispatcher.Event SHUFFLE = new Dispatcher.Event("Shuffle");
    public static final Dispatcher.Event SHUFFLE_PLAY = new Dispatcher.Event("ShufflePlay");
    public static final Dispatcher.Event SHUFFLE_PLAY_ARTIST = new Dispatcher.Event("ShufflePlayArtist");

    public static final Dispatcher.Event PREPARE_FIRST_SONG = new Dispatcher.Event("PrepareFirstSong");
    public static final Dispatcher.Event PLAY_PAUSE_CURRENT = new Dispatcher.Event("PlayPauseCurrent");
    public static final Dispatcher.Event PAUSE = new Dispatcher.Event("Pause");
    public static final Dispatcher.Event SKIP_NEXT = new Dispatcher.Event("SkipNext");
    public static final Dispatcher.Event SKIP_PREVIOUS = new Dispatcher.Event("SkipPrevious");
    public static final Dispatcher.Event FINISHED_PLAYING = new Dispatcher.Event("FinishedPlaying");
    public static final Dispatcher.Event TOGGLE_DURATION_REMAINING = new Dispatcher.Event("ToggleDurationRemaining");
}