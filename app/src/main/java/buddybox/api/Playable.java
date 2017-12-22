package buddybox.api;

public interface Playable {

    class Play extends Core.Event {
        public final int playableId;

        public Play(int playableId) {
            this.playableId = playableId;
        }
    }

    String name();
    String subtitle();

    Play play();
}
