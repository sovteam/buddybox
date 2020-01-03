package buddybox.io;

import android.net.Uri;

public class SongMedia {

    String audioTitle;
    String audioDuration;
    String audioArtist;
    Uri audioUri;
    
    public String getTitle() {
        return audioTitle;
    }

    public void setTitle(String audioTitle) {
        this.audioTitle = audioTitle;
    }

    public String getDuration() {
        return audioDuration;
    }

    public void setDuration(String audioDuration) {
        this.audioDuration = audioDuration;
    }

    public String getaudioArtist() {
        return audioArtist;
    }

    public void setArtist(String audioArtist) {
        this.audioArtist = audioArtist;
    }
    
    public Uri getUri() {
        return audioUri;
    }

    public void setUri(Uri audioUri) {
        this.audioUri = audioUri;
    }
    
}