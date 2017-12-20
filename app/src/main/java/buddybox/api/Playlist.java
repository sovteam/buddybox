package buddybox.api;

import java.util.List;

public class Playlist implements Playable {
    public final int id;
    public final String name;
    public final List<Song> songs;

    public Playlist(int id, String name, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.songs = songs;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String subtitle() {
        return "Playlist with " + songs.size() + " songs";
    }
}
