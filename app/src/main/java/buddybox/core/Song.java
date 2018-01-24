package buddybox.core;

import java.io.File;

public class Song implements Playable {

    public final Hash hash;
    public final String name;
    public final String artist;
    public final String genre;
    public final Integer duration;

    public boolean isMissing = false;

    public Long loved;
    public Boolean lovedViewed;

    public String relativePath;
    public final long fileLength;
    public final long lastModified;


    public Song(Hash hash, String name, String artist, String genre, Integer duration, String relativePath,
                long fileLength, long lastModified) {
        this.hash = hash;
        this.name = name;
        this.artist = artist;
        this.genre = genre;
        this.duration = duration;
        this.relativePath = relativePath;
        this.fileLength = fileLength;
        this.lastModified = lastModified;
    }

    @Override public String name() { return name; }
    @Override public String subtitle() { return artist; }
    @Override public String duration() {
        int minutes = duration / 1000 / 60;
        int seconds = duration / 1000 % 60;
        return  minutes + ":" + (seconds < 10 ? "0" + seconds : seconds);
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
    }
}
