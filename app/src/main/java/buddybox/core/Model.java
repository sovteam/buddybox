package buddybox.core;

public interface Model {

    void addStateListener(StateListener listener);

    interface StateListener {
        void update(State state);
    }

}
