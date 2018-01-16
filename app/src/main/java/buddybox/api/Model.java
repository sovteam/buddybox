package buddybox.api;

public interface Model {

    void dispatch(Event event);

    void setStateListener(StateListener listener);

    interface StateListener {
        void update(VisibleState state);
    }

    class Event {
        public final String type;

        public Event(String type) {
            this.type = type;
        }
    }

}
