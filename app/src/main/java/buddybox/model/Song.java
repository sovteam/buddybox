package buddybox.model;

public class Song {

    public final int id;
    public final Hash hash;
    public final String name;
    public final String artist;
    public final String genre;
    public final Integer duration;
    public boolean isMissing = false;

    public Long loved;
    public Boolean lovedViewed;
    public String relativePath;

    public Song(int id, Hash hash, String name, String artist, String genre, Integer duration, String relativePath) {
        this.id = id;
        this.hash = hash;
        this.name = name;
        this.artist = artist;
        this.genre = genre;
        this.duration = duration;
        this.relativePath = relativePath;
    }

    public String name() { return name; }
    public String subtitle() { return artist; }
    public String duration() {
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
