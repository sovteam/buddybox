package buddybox.api;

public class AddSongToPlaylist extends Model.Event {
    private final String songId;
    private final String playlistName;

    public AddSongToPlaylist(String songId, String playlistName) {
        super("AddSongToPlaylist songId: " + songId + ", playlist: " + playlistName);
        this.songId = songId;
        this.playlistName = playlistName;
    }

}