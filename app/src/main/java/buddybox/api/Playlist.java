package buddybox.api;

import java.util.List;

import buddybox.impl.SongImpl;

public class Playlist implements Playable {

    private final int id;
    public final String name;
    public int currentSongIndex;
    public final List<Song> songs;

    public Playlist(int id, String name, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.songs = songs;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String subtitle() {
        return "Playlist with " + songs.size() + " songs";
    }

    public SongImpl song(int songIndex) {
        return songIndex >= songs.size()
            ? null
            : (SongImpl)songs.get(songIndex);
    }

    public Integer songAfter(int songIndex, int step) {
        return songs.size() == 0
            ? null
            : (songs.size() + songIndex + step) % songs.size();
    }

    public boolean isEmpty() {
        return this.songs == null || this.songs.isEmpty();
    }

    public int size() {
        if (songs == null)
            return 0;
        return songs.size();
    }

    public void removeSong(int songIndex) {
        if (songIndex > this.songs.size())
            return;
        this.songs.remove(songIndex);
    }
}
