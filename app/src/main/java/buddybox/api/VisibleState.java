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
    public final Song sampling;
    public final int newSamplerSongsCount;

    // Loved
    public final List<Song> lovedSongs;

    // Library
    public final Playlist recent;
    public final List<Playlist> playlists;
    public final List<Playable> searchResults;
    public final long availableMemorySize;


    public VisibleState(int songCount, String[] musicFolders, Song playing, Playlist playlistPlaying, boolean isPaused, Map<Song, Playlist> playlistBySong, Song sampling, int newSamplerSongsCount, Playlist recent, List<Playlist> playlists, List<Playable> searchResults, int buddyCount, long availableMemorySize, List<Song> lovedSongs) {
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
        this.availableMemorySize = availableMemorySize;
        this.newSamplerSongsCount = newSamplerSongsCount;
    }
}
