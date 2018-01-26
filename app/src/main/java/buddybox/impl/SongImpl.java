package buddybox.impl;

import java.io.File;

public class SongImpl extends Song {

    // TODO remove SongImpl

    public File file;

    public SongImpl(int id, Hash hash, String name, String artist, String genre, Integer duration, File file) {
        super(id, hash, name, artist, genre, duration, file.getPath());
        this.file = file;
    }

}
