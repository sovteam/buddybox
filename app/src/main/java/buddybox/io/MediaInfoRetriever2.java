package buddybox.io;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

import static buddybox.core.Dispatcher.dispatch;

import buddybox.core.Artist;
import buddybox.core.Playable;
import buddybox.core.Song;
import buddybox.core.events.AlbumArtEmbeddedFound;

public class MediaInfoRetriever2 {

    private static String TAG = MediaInfoRetriever2.class.getSimpleName();

    private static String ASSETS_FOLDER_PATH = Environment.DIRECTORY_MUSIC + File.separator + "Assets";
    private static File ALBUMS_FOLDER  = getAssetFolder(ASSETS_FOLDER_PATH + File.separator + "Albums");
    private static File ARTISTS_FOLDER = getAssetFolder(ASSETS_FOLDER_PATH + File.separator + "Artists");

    private static final String API_KEY = "c65adb3fdfa66e16cb4308ad76f2a052";

    public static Bitmap load(Playable playable) {
        if (playable instanceof Song) {
            Song song = (Song)playable;
            if (song.hasEmbeddedArt() == Boolean.FALSE)
                return loadExternalArt(song);
            else {
                Bitmap bitmap = getEmbeddedBitmap(song);
                boolean wasFound = bitmap != null;
                if (song.hasEmbeddedArt() == null)
                    dispatch(new AlbumArtEmbeddedFound(song, wasFound));
                return wasFound
                    ? bitmap
                    : loadExternalArt(song);
            }
        } else
            return loadExternalArt((Artist)playable); //TODO Artist
    }

    private static Bitmap loadExternalArt(Artist artist) {
        //TODO
        return null;
    }

    private static Bitmap loadExternalArt(Song song) {
        Bitmap fromLastFM = loadAlbumArtFromLastFM(song);
        return (fromLastFM != null)
            ? fromLastFM
            : loadAlbumArtFromFolder(song);
    }



    private static Bitmap loadAlbumArtFromFolder(Song song) {

        //TODO: Remove image file extensions to avoid fullscan and open file directly.

        final String fileName = albumArtFileName(song.artist, song.album);
        File[] images = ALBUMS_FOLDER.listFiles(new FilenameFilter() { @Override public boolean accept(File dir, String name) {
            return name.equals(fileName + ".png") || name.equals(fileName + ".jpg") || name.equals(fileName + ".jpeg");
        }});

        return (images != null && images.length > 0)
            ? BitmapFactory.decodeFile(images[0].getPath())
            : null;
    }


    private static Bitmap loadAlbumArtFromLastFM(Song song) {
        return null; // TODO
    }



    static Bitmap getEmbeddedBitmap(Song song) {
        byte[] picture = getEmbeddedPicture(song);

        return (picture == null)
                ? null
                : BitmapFactory.decodeByteArray(picture, 0, picture.length);
    }

    private static byte[] getEmbeddedPicture(Song song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(song.filePath);
            return retriever.getEmbeddedPicture();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            retriever.release();
        }
    }

    private static File getAssetFolder(String folderPath) {
        File folder = Environment.getExternalStoragePublicDirectory(folderPath);
        if (!folder.exists())
            if (!folder.mkdirs())
                Log.d(TAG, "Unable to create folder: " + folder);
        return folder;
    }

    private static String encode(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9-_\\.]", "_").toLowerCase();
    }

    private static String albumArtFileName(String artist, String album) {
        return encode(artist + "-" + album).toLowerCase();
    }

}
