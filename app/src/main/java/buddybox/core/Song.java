package buddybox.core;

import java.util.Locale;

import utils.Hash;

public class Song implements Playable {

    public final Long id;
    public final Hash hash;
    public String name;
    public String artist;
    public String album;
    public String genre;
    public final Integer duration;

    // sampler
    public boolean isSample;

    public Long loved;
    public Boolean lovedViewed;
    public Long hated;
    public Long deleted;
    public String filePath;

    public final long fileLength;
    public final long lastModified;

    public boolean isMissing;
    public boolean isDeleted;

    public Song(Long id, Hash hash, String name, String artist, String album, String genre, Integer duration, String filePath,
                long fileLength, long lastModified, boolean isMissing, boolean isDeleted) {
        this.id = id;
        this.hash = hash;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.duration = duration;
        this.filePath = filePath;
        this.fileLength = fileLength;
        this.lastModified = lastModified;
        this.isMissing = isMissing;
        this.isDeleted = isDeleted;
    }

    @Override public String name() { return name; }
    @Override public String subtitle() { return artist; }
    @Override
    public String duration() {
        return formatTime(duration);
    }

    public String formatTime(int time) {
        int minutes = time / 1000 / 60;
        int seconds = time / 1000 % 60;
        return  minutes + ":" + (seconds < 10 ? "0" + seconds : seconds);
    }

    @Override
    public boolean equals(Object obj) {
        return  obj != null &&
                obj.getClass() == Song.class &&
                hash.equals(((Song)obj).hash);
    }

    public void setLoved() {
        loved = System.currentTimeMillis();
        lovedViewed = false;
    }

    public boolean isLoved() {
        return loved != null;
    }

    public void setLovedViewed() {
        lovedViewed = true;
    }

    public boolean isLovedViewed() {
        return lovedViewed != null && lovedViewed;
    }

    public void setMissing() {
        isMissing = true;
    }

    public void setNotMissing() {
        isMissing = false;
        isDeleted = false;
    }

    public void setDeleted() {
        isDeleted = true;
        setMissing();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String printFileLength() {
        return String.format(Locale.getDefault(), "%.1f", (double) fileLength / 1024 / 1024) + " MB";
    }
}
