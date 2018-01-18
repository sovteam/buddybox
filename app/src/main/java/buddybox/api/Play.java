package buddybox.api;

public class Play extends Model.Event {
    public final int songIndex;
    public final Playlist playlist;

    public Play(Playlist playlist, int songIndex) {
        super ("Play");
        this.playlist = playlist;
        this.songIndex = songIndex;
    }

    public static final Model.Event PLAY_PAUSE_CURRENT = new Model.Event("PlayPauseCurrent");
    public static final Model.Event SKIP_NEXT = new Model.Event("SkipNext");
    public static final Model.Event SKIP_PREVIOUS = new Model.Event("SkipPrevious");

    public static final Model.Event FINISHED_PLAYING = new Model.Event("FinishedPlaying");

}