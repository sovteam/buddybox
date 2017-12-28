package buddybox.api;

public class Play extends Core.Event {
    public final int songIndex;
    public final Playlist playlist;

    public Play(Playlist playlist, int songIndex) {
        super ("Play");
        this.playlist = playlist;
        this.songIndex = songIndex;
    }

    public static final Core.Event PLAY_PAUSE_CURRENT = new Core.Event("PlayPauseCurrent");
    public static final Core.Event SKIP_NEXT = new Core.Event("SkipNext");
    public static final Core.Event SKIP_PREVIOUS = new Core.Event("SkipPrevious");

}