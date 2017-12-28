package buddybox.impl;

import java.io.File;

import buddybox.api.Song;

public class SongImpl extends Song {

    public final File file;

    public SongImpl(int id, String name, String artist, String genre, File file) {
        super(id, name, artist, genre);
        this.file = file;
    }
}
