package buddybox.core;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Artist extends Playlist {

    public Bitmap picture;
    private Map<String, List<Song>> songsByAlbum;
    private String bio;

    public Artist(long id, String name, String bio, Long lastPlayed) {
        super(id, name, lastPlayed, new ArrayList<Song>());
        this.bio = bio;
    }

    public void addSong(Song song) {
        super.addSong(song);

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

    public String songsCountPrint() {
        int count = size();
        String ret = Integer.toString(count) + " song";
        if (count != 1)
            ret += "s";
        return ret;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    public void removeSong(Song song) {
        super.removeSong(song);

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

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBio() {
        return bio;
    }
}
