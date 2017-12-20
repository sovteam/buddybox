package buddybox.api;

public class Song implements Playable {
    public final int id;
    public final String name;
    public final String artist;
    public final String genre;

    public Song(int id, String name, String artist, String genre) {
        this.id = id;
        this.name = name;
        this.artist = artist;
        this.genre = genre;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String subtitle() {
        return artist;
    }

    public Play play() {
        return new Play(id);
    }

    public class Play extends Core.Event {
        public final int songID;

        public Play(int songID) {
            this.songID = songID;
        }
    }
}