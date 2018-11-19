package buddybox.io;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import buddybox.core.Artist;
import buddybox.core.Playable;
import buddybox.core.Song;
import buddybox.core.events.AlbumArtEmbeddedFound;
import buddybox.core.events.AlbumArtRequested;
import buddybox.web.DownloadUtils;
import buddybox.web.HttpUtils;

import static buddybox.core.Dispatcher.dispatch;

public class MediaInfoRetriever2 {

    private static final long ONE_MINUTE = 1000 * 60;
    private static final long ONE_MONTH  = ONE_MINUTE * 60 * 24 * 30;
    private static String TAG = MediaInfoRetriever2.class.getSimpleName();

    private static String ASSETS_FOLDER_PATH = Environment.DIRECTORY_MUSIC + File.separator + "Assets";
    private static File ALBUMS_FOLDER  = produceAssetFolder(ASSETS_FOLDER_PATH + File.separator + "Albums");
    private static File ARTISTS_FOLDER = produceAssetFolder(ASSETS_FOLDER_PATH + File.separator + "Artists");

    private static final String API_KEY = "c65adb3fdfa66e16cb4308ad76f2a052";
    private static Application context;

    private static long lastDownloadError;

    public static void init(Application context) {
        MediaInfoRetriever2.context = context;
    }

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
        File file = new File(ALBUMS_FOLDER, albumArtFileName(song.artist, song.album));
        if (!file.exists()) return null;
        return BitmapFactory.decodeFile(file.getPath());
    }

    private static Bitmap loadAlbumArtFromLastFM(Song song) {
        System.out.println(">>> loadFromLastFM: " + song.artist + "::" + song.album);
        if (System.currentTimeMillis() - song.lastAlbumArtRequested < ONE_MONTH)
            return null;
        if (!Network.hasConnection(context))
            return null;
        if (System.currentTimeMillis() - lastDownloadError < ONE_MINUTE)
            return null;

        String artist = Uri.encode(song.artist);
        String album = Uri.encode(song.album);
        String urlString = "https://ws.audioscrobbler.com/2.0/?method=album.getinfo&artist=" + artist + "&album=" + album + "&autocorrect=1&api_key=" + API_KEY + "&format=json";

        Bitmap art = null;
        try {
            JSONObject response = HttpUtils.getHttpResponse(urlString);
            String imageUrl = getAlbumImageUrl(response);
            if (imageUrl != null) {
                File toWrite = new File(ALBUMS_FOLDER, albumArtFileName(song.artist, song.album));
                art = downloadImage(imageUrl, toWrite);
                System.out.println(">>> AlbumArt FOUND: " + song.album);
            }
        } catch (Exception e) {
            lastDownloadError = System.currentTimeMillis();
            System.out.println(">>> AlbumArt ERROR: " + song.album);
            return null;
        }

        dispatch(new AlbumArtRequested(song.artist, song.album));
        return art;
    }

    private static String albumArtFileName(String artist, String album) {
        return encode(artist + "-" + album).toLowerCase();
    }

    private static String getAlbumImageUrl(JSONObject json) {
        // fetch response
        String ret;
        try {
            if (!json.has("album"))
                return null;
            JSONObject album = json.getJSONObject("album");

            if (!album.has("image"))
                return null;
            JSONArray images = album.getJSONArray("image");

            if (images.length() == 0)
                return null;
            // get largest image. TODO proper select image by size
            JSONObject image = images.getJSONObject(images.length() - 1);

            if (!image.has("#text"))
                return null;
            ret = image.getString("#text").trim();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        if (ret.isEmpty())
            return null;
        return ret;
    }

    private static Bitmap downloadImage(String urlStr, File toWrite) throws IOException {
        // download image
        byte[] response = DownloadUtils.downloadFile(urlStr);
        if (response == null)
            return null;

        // save downloaded image
        FileOutputStream fos = new FileOutputStream(toWrite);
        fos.write(response);
        fos.close();

        return BitmapFactory.decodeFile(toWrite.getPath());
    }



    private static Bitmap getEmbeddedBitmap(Song song) {
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

    private static File produceAssetFolder(String folderPath) {
        File folder = Environment.getExternalStoragePublicDirectory(folderPath);
        if (!folder.exists())
            if (!folder.mkdirs())
                Log.d(TAG, "Unable to create folder: " + folder);
        return folder;
    }

    private static String encode(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9-_\\.]", "_").toLowerCase();
    }

}
