package buddybox.core;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Artist {

    public final String name;
    public Set<Song> songs;
    public Bitmap picture;
    private Map<String, List<Song>> songsByAlbum;

    public Artist(String name) {
        this.name = name;
        this.songs = new HashSet<>();
    }

    public void addSong(Song song) {
        songs.add(song);

        if (songsByAlbum != null)
            addSongByAlbum(song);
    }

    private void addSongByAlbum(Song song) {
        List<Song> albumSongs = songsByAlbum.get(song.album);
        if (albumSongs == null)
            albumSongs = new ArrayList<>();
        albumSongs.add(song);
        songsByAlbum.put(song.album, albumSongs); // TODO sort songs by track number
    }

    public int songsCount() {
        return songs.size();
    }

    public String songsCountPrint() {
        int count = songsCount();
        String ret = Integer.toString(count) + " song";
        if (count != 1)
            ret += "s";
        return ret;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    public void removeSong(Song song) {
        songs.remove(song);

        if (songsByAlbum != null)
            removeSongFromAlbum(song);
    }

    private void removeSongFromAlbum(Song song) {
        List<Song> albumSongs = songsByAlbum.get(song.album);
        albumSongs.remove(song);
        if (albumSongs.size() == 0)
            songsByAlbum.remove(song.album);
        else
            songsByAlbum.put(song.album, albumSongs);
    }

    public Map<String, List<Song>> songsByAlbum() {
        if (songsByAlbum == null) {
            songsByAlbum = new HashMap<>();
            for (Song song : songs)
                addSongByAlbum(song);
        }
        return songsByAlbum;
    }


    public List<Song> getAlbumSongs(String album) {
        return songsByAlbum().get(album);
    }
}
