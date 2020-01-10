package buddybox.io;

import android.net.Uri;

public class SongMedia {

    private String title;
    private String duration;
    private String artist;
    private String album;
    private Uri uri;
    private long modified;

    public String getTitle() {
        return title;
    }

    public void setTitle(String audioTitle) {
        this.title = audioTitle;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String audioDuration) {
        this.duration = audioDuration;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String audioArtist) {
        this.artist = audioArtist;
    }
    
    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri audioUri) {
        this.uri = audioUri;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long currentModified) {
        this.modified = currentModified;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }
}