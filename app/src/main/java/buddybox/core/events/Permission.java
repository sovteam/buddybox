package buddybox.core.events;

import buddybox.core.Dispatcher;

public class Permission extends Dispatcher.Event {
    public static final int WRITE_EXTERNAL_STORAGE = 1;

    public final int code;
    public final boolean granted;

    public Permission(int code, boolean granted) {
        super("Permission " + code);
        this.code = code;
        this.granted = granted;
    }
}
