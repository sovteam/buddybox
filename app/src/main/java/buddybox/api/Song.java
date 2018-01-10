package buddybox.api;

public class Song implements Playable {

    public final int id;
    public final String name;
    public final String artist;
    public final String genre;

    public Long loved;

    public Song(int id, String name, String artist, String genre) { this.id = id; this.name = name; this.artist = artist; this.genre = genre; }

    @Override public String name() { return name; }
    @Override public String subtitle() { return artist; }

    public void setLoved() {
        loved = System.currentTimeMillis();
    }

    public boolean isLoved() {
        return loved != null;
    }

}