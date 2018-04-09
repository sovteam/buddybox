package buddybox.core.events;

import buddybox.core.Dispatcher;

public class ArtistSelectedByName extends Dispatcher.Event {
    public final String name;

    public ArtistSelectedByName(String name) {
        super("ArtistSelectedByName: " + name);
        this.name = name;
    }
}
