package buddybox.core;

import java.util.List;
import java.util.Map;

public class State {

    // Sharing
    public final int buddyCount;
    public final int songCount;
    public final String[] musicFolders;

    // Playing
    public final Song songPlaying;
    public final Playlist playlistPlaying;
    public final boolean isPaused;
    public final Map<String, List<Playlist>> playlistsBySong;

    // Editing
    // public final String songNameError;
    // public final String artistNameError;
    // public final String genreNameError;

    // Sampler
    public final Playlist samplerPlaylist;

    // Loved
    public final Playlist lovedPlaylist;

    // Library
    public final Playlist allSongsPlaylist;
    public final List<Playlist> playlists;
    public final List<Playable> searchResults;
    public final long availableMemorySize;
    public final long mediaStorageUsed;
    public final List<Artist> artists;
    public final boolean isSampling;
    public final boolean repeatAll;
    public final boolean repeatSong;
    public final boolean syncLibraryPending;
    public final Song deleteSong;

    // Player
    public final Playlist selectedPlaylist;
    public final Integer seekTo;
    public final boolean isShuffle;
    public final Song selectedSong;
    public final int speakerVolume;
    public final int headphonesVolume;
    public final String outputActive;

    public State(int songCount, String[] musicFolders, Song playing, Playlist playlistPlaying, Integer seekTo, boolean isPaused, boolean isShuffle, boolean repeatAll, Boolean repeatSong, Map<String, List<Playlist>> playlistsBySong, boolean isSampling, Playlist samplerPlaylist, Playlist lovedPlaylist, List<Playlist> playlists, List<Playable> searchResults, int buddyCount, long availableMemorySize, Long mediaStorageUsed, Playlist recent, List<Artist> artists, boolean syncLibraryRequested, Song deleteSong, Playlist selectedPlaylist, Song selectedSong, String outputActive, int speakerVolume, int headphonesVolume) {
        this.seekTo = seekTo;
        this.isShuffle = isShuffle;
        this.outputActive = outputActive;
        this.buddyCount = buddyCount;
        this.songCount = songCount;
        this.musicFolders = musicFolders;

        this.songPlaying = playing;
        this.playlistPlaying = playlistPlaying;
        this.isPaused = isPaused;
        this.repeatAll = repeatAll;
        this.repeatSong = repeatSong;
        this.playlistsBySong = playlistsBySong;

        this.isSampling = isSampling;
        this.samplerPlaylist = samplerPlaylist;
        this.lovedPlaylist = lovedPlaylist;

        this.allSongsPlaylist = recent;
        this.artists = artists;
        this.playlists = playlists;
        this.searchResults = searchResults;
        this.availableMemorySize = availableMemorySize;
        this.mediaStorageUsed = mediaStorageUsed;
        this.syncLibraryPending = syncLibraryRequested;
        this.deleteSong = deleteSong;
        this.selectedPlaylist = selectedPlaylist;
        this.selectedSong = selectedSong;
        this.speakerVolume = speakerVolume;
        this.headphonesVolume = headphonesVolume;
    }
}
