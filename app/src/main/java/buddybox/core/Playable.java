package buddybox.core;

public interface Playable {

    String name();
    String subtitle();
    String duration();
    Long lastPlayed();

    void updateLastPlayed(long time);
}
