package buddybox.core;

public interface IModel {

    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    interface StateListener {
        void update(State state);
    }

}
