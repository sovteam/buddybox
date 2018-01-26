package buddybox.core;

public interface IModel {

    void addStateListener(StateListener listener);

    interface StateListener {
        void update(State state);
    }

}
