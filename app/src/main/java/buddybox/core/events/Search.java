package buddybox.core.events;

import buddybox.core.Dispatcher;

public class Search extends Dispatcher.Event {

    public final String searchText;

    public Search(String searchText) {
        super("Search text: " + searchText);
        this.searchText = searchText;
    }
}
