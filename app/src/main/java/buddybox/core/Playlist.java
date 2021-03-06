package buddybox.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Playlist implements Playable {

    private static Random RANDOM_SEED = null;

    private final long id;
    public String name;
    public List<Song> songs;
    private List<Integer> shuffledSongs;
    private Long lastPlayed;

    public Playlist(long id, String name, Long lastPlayed, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.lastPlayed = lastPlayed;
        this.songs = songs;
    }

    public static void setRandomSeed(int randomSeed) {
        // Use for tests only
        Playlist.RANDOM_SEED = new Random(randomSeed);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String subtitle() {
        String songsPrint = size() + " song" + (size() != 1 ? "s" : "");
        if (size() > 0)
            return songsPrint + " - " + duration();
        return songsPrint;
    }

    @Override
    public String duration() {
        int total = 0;
        for (Song song : songs)
            total += song.duration;
        return formatDuration(total);
    }

    @Override
    public Long lastPlayed() {
        return lastPlayed;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void updateLastPlayed(long time) {
        lastPlayed = time;
    }

    private String formatDuration(int milliseconds) {
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
        return song(songIndex, false);
    }

    public Song song(int songIndex, boolean isShuffle) {
        if (size() == 0)
            return null;

        int index = isShuffle
                ? shuffledSongs().get(songIndex)
                : songIndex;
        return songs.get(index);
    }

    public Integer songAfter(int songIndex, int step) {
        int total = size();
        if (total == 0)
            return null;

        return (total + songIndex + step) % total;
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

    public void removeSong(Song song) {
        if (shuffledSongs != null) {
            int songIndex = songs.indexOf(song);
            shuffledSongs.remove(songIndex);
        }
        songs.remove(song);
    }

    public void addSong(Song song) {
        songs.add(song);
        if (shuffledSongs != null) {
            shuffledSongs.add(songs.size() -1);
        }
    }

    public boolean hasSong(Song song) {
        return songs.contains(song);
    }

    public boolean isLastSong(Integer songIndex, boolean isShuffle) {
        return isShuffle
                ? shuffledSongs().indexOf(songIndex) +1 == shuffledSongs.size()
                : songIndex +1 == size();
    }

    public int firstShuffleIndex() {
        return shuffledSongs(true).get(0);
    }

    private List<Integer> shuffledSongs() {
        return shuffledSongs(false);
    }

    private List<Integer> shuffledSongs(Boolean reset) {
        if (shuffledSongs == null || reset) {
            shuffledSongs = new ArrayList<>();
            for (int i = 0; i < size(); i++)
                shuffledSongs.add(i);

            if (RANDOM_SEED == null)
                Collections.shuffle(shuffledSongs);
            else
                Collections.shuffle(shuffledSongs, RANDOM_SEED);
        }
        return shuffledSongs;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void changeSongPosition(int fromPosition, int toPosition) {
        Song song = song(fromPosition);
        songs.remove(song);
        songs.add(toPosition, song);
    }

    public int indexOf(Song song, boolean isShuffle) {
        int ret = songs.indexOf(song);
        return isShuffle
                ? shuffledSongs().indexOf(ret)
                : ret;
    }

}
