package buddybox.api;

public class CreatePlaylist extends Model.Event {
    private final String playlistName;
    private final String songId;

    public CreatePlaylist(String playlistName, String songId) {
        super("CreatePlaylist playlistName: " + playlistName + ", songId: " + songId);
        this.playlistName = playlistName;
        this.songId = songId;
    }
}
