package buddybox.core;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Artist extends Playlist {

    public Bitmap picture;
    private String bio;

    public Artist(long id, String name, String bio, Long lastPlayed) {
        super(id, name, lastPlayed, new ArrayList<Song>());
        this.bio = bio;
    }

    public String songsCountPrint() {
        int count = size();
        String ret = Integer.toString(count) + " song";
        if (count != 1)
            ret += "s";
        return ret;
    }

    public void setPicture(Bitmap picture) {
        this.picture = picture;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBio() {
        return bio;
    }
}
