package buddybox.impl;

import java.io.File;

import buddybox.api.Song;

public class SongImpl extends Song {

    public File file;

    public SongImpl(int id, String name, String artist, String genre, Integer duration, File file) {
        super(id, name, artist, genre, duration);
        this.file = file;
    }
}
