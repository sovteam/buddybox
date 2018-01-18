package buddybox.api;

public interface Model {

    void dispatch(Event event);

    void addStateListener(StateListener listener);

    interface StateListener {
        void update(State state);
    }

    class Event {
        public final String type;

        public Event(String type) {
            this.type = type;
        }
    }

}
