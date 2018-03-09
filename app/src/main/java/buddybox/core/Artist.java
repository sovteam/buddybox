package buddybox.core;

import android.graphics.Bitmap;

import java.util.HashSet;
import java.util.Set;

public class Artist {

    public final String name;
    public Set<Song> songs;
    public Bitmap picture;

    public Artist(String name) {
        this.name = name;
        this.songs = new HashSet<>();
    }

    public void addSong(Song song) {
        songs.add(song);
    }

    public int songsCount() {
        return songs.size();
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }
}
