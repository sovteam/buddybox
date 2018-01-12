package buddybox.api;

import java.util.List;
import java.util.Map;

public class VisibleState {

    // Sharing
    public final int buddyCount;
    public final int songCount;
    public final String[] musicFolders;

    // Playing
    public final Song songPlaying;
    public final Playlist playlistPlaying;
    public final boolean isPaused;
    public final Map<Song, Playlist> playlistBySong;

    // Editing
    // public final String songNameError;
    // public final String artistNameError;
    // public final String genreNameError;

    // Sampler
    public final Playlist samplerPlaylist;

    // Loved
    public final Playlist lovedPlaylist;

    // Library
    public final Playlist recentPlaylist;
    public final List<Playlist> playlists;
    public final List<Playable> searchResults;
    public final long availableMemorySize;


    public VisibleState(int songCount, String[] musicFolders, Song playing, Playlist playlistPlaying, boolean isPaused, Map<Song, Playlist> playlistBySong, Playlist samplerPlaylist, Playlist lovedPlaylist, List<Playlist> playlists, List<Playable> searchResults, int buddyCount, long availableMemorySize, Playlist recent) {
        this.buddyCount = buddyCount;
        this.songCount = songCount;
        this.musicFolders = musicFolders;

        this.songPlaying = playing;
        this.playlistPlaying = playlistPlaying;
        this.isPaused = isPaused;
        this.playlistBySong = playlistBySong;

        this.samplerPlaylist = samplerPlaylist;
        this.lovedPlaylist = lovedPlaylist;

        this.recentPlaylist = recent;
        this.playlists = playlists;
        this.searchResults = searchResults;
        this.availableMemorySize = availableMemorySize;
    }
}
