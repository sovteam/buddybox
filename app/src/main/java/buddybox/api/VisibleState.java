package buddybox.api;

import java.util.List;
import java.util.Map;

public class VisibleState {

    public enum MainFrames { LIBRARY, SAMPLER, LOVED, BUDDIES }
    public final MainFrames selectedFrame;

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

    // Radio
    public final Song sampling;

    // Loved
    public final List<Song> lovedSongs;

    // Library
    public final Playlist recent;
    public final List<Playlist> playlists;
    public final List<Playable> searchResults;


    public VisibleState(MainFrames selectedFrame, int songCount, String[] musicFolders, Song playing, Playlist playlistPlaying, boolean isPaused, Map<Song, Playlist> playlistBySong, Song sampling, List<Song> lovedSongs, Playlist recent, List<Playlist> playlists, List<Playable> searchResults, int buddyCount) {
        this.buddyCount = buddyCount;
        this.songCount = songCount;
        this.musicFolders = musicFolders;

        this.songPlaying = playing;
        this.playlistPlaying = playlistPlaying;
        this.isPaused = isPaused;
        this.playlistBySong = playlistBySong;

        this.sampling = sampling;

        this.lovedSongs = lovedSongs;

        this.recent = recent;
        this.playlists = playlists;
        this.searchResults = searchResults;
        this.selectedFrame = selectedFrame;
    }
}
