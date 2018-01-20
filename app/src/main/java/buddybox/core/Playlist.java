package buddybox.core;

import java.util.List;

public class Playlist implements Playable {

    private final int id;
    public final String name;
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
        return songs.size() + " songs";
    }

    @Override
    public String duration() {
        int total = 0;
        for (Song song : songs)
            total += song.duration;
        return formatDuration(total);
    }

    public String formatDuration(int milliseconds) {
        int duration = milliseconds / 1000;
        int hours = duration / 60 / 60;
        int minutes = (duration - hours * 60 * 60) / 60;
        int seconds = duration - hours * 60 * 60 - minutes * 60;

        String ret = hours > 0 ? Integer.toString(hours) + ":" : "";
        ret += !ret.isEmpty() && minutes < 10 ? "0" + minutes : minutes;
        ret += ":" + (seconds < 10 ? "0" + seconds : seconds);
        return  ret;
    }

    public Song song(int songIndex) {
        return songIndex >= songs.size()
            ? null
            : songs.get(songIndex);
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
