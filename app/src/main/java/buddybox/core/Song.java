package buddybox.core;

import utils.Hash;

public class Song implements Playable {

    public final Hash hash;
    public final String name;
    public final String artist;
    public final String genre;
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


    public Song(Hash hash, String name, String artist, String genre, Integer duration, String filePath,
                long fileLength, long lastModified, boolean isMissing, boolean isDeleted) {
        this.hash = hash;
        this.name = name;
        this.artist = artist;
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
    @Override public String duration() {
        int minutes = duration / 1000 / 60;
        int seconds = duration / 1000 % 60;
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
}
