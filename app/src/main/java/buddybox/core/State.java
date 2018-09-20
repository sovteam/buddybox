package buddybox.core;

import java.util.List;
import java.util.Map;

import buddybox.model.AlbumInfo;

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
    public final List<Song> allSongs;
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
    public final Boolean isStopped;
    public final boolean isShuffle;
    public final Song selectedSong;
    public final boolean showDuration;
    public final String outputActive;
    public final Map<String, Integer> volumeSettings;
    public final boolean hasAudioFocus;
    public final Artist artistSelected;
    public final Map<String, Album> artistAlbums;
    public final Song songToCheckEmbeddedArt;
    public final AlbumInfo albumToFindArt;
    public final Artist artistToFindInfo;
    public final List<Playable> recent;

    public State(int songCount, String[] musicFolders, List<Playable> recentList, Song playing, Playlist playlistPlaying, Integer seekTo, Boolean isStopped, boolean isPaused, boolean isShuffle, boolean repeatAll, Boolean repeatSong, boolean showDuration, Map<String, List<Playlist>> playlistsBySong, boolean isSampling, Playlist samplerPlaylist, Playlist lovedPlaylist, List<Playlist> playlists, List<Playable> searchResults, int buddyCount, long availableMemorySize, Long mediaStorageUsed, List<Song> allSongs, List<Artist> artists, boolean syncLibraryRequested, Song deleteSong, Playlist selectedPlaylist, Song selectedSong, String outputActive, Map<String, Integer> volumeSettings, boolean hasAudioFocus, Artist artistSelected, Map<String, Album> artistAlbums, Song songToCheckEmbeddedArt, AlbumInfo albumToFindArt, Artist artistToFindInfo) {
        this.seekTo = seekTo;
        this.isStopped = isStopped;
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
        this.showDuration = showDuration;
        this.playlistsBySong = playlistsBySong;

        this.isSampling = isSampling;
        this.samplerPlaylist = samplerPlaylist;
        this.lovedPlaylist = lovedPlaylist;

        this.recent = recentList;
        this.allSongs = allSongs;
        this.artists = artists;
        this.playlists = playlists;
        this.searchResults = searchResults;
        this.availableMemorySize = availableMemorySize;
        this.mediaStorageUsed = mediaStorageUsed;
        this.syncLibraryPending = syncLibraryRequested;
        this.deleteSong = deleteSong;
        this.selectedPlaylist = selectedPlaylist;
        this.selectedSong = selectedSong;
        this.volumeSettings = volumeSettings;
        this.hasAudioFocus = hasAudioFocus;
        this.artistSelected = artistSelected;
        this.artistAlbums = artistAlbums;
        this.songToCheckEmbeddedArt = songToCheckEmbeddedArt;
        this.albumToFindArt = albumToFindArt;
        this.artistToFindInfo = artistToFindInfo;
    }
}
