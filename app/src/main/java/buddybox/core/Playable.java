package buddybox.core;

public interface Playable {

    String name();
    String subtitle();
    String duration();
    Long lastPlayed();
    Long getId();

    void updateLastPlayed(long time);
}
