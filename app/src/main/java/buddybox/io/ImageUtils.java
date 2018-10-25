package buddybox.io;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import buddybox.core.Playable;
import buddybox.core.Song;

public class ImageUtils {

    //TODO Cache last BitmapByPlayable (300 or so)

    public static Bitmap load(Playable playable) {
        if (playable instanceof Song) {
            Song song = (Song) playable;
            return song.hasEmbeddedArt()
                    ? getEmbeddedBitmap(song)
                    : loadSongBitmap(song);
        } else
            return null;
    }

    private static Bitmap loadSongBitmap(Song song) {
        System.out.println("LOAD SONG BITMAP");
        return null;
    }

    static Bitmap getEmbeddedBitmap(Song song) {
        byte[] picture = getEmbeddedPicture(song);

        return (picture == null)
                ? null
                : BitmapFactory.decodeByteArray(picture, 0, picture.length);
    }

    private static byte[] getEmbeddedPicture(Song song) {
        MediaMetadataRetriever retriever;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(song.filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            retriever.release();
        }
    }
}
