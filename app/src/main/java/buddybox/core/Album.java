package buddybox.core;

import java.util.ArrayList;

public class Album extends Playlist {

    public Album(long id, String name, Long lastPlayed) {
        super(id, name, lastPlayed, new ArrayList<Song>());
    }
}
