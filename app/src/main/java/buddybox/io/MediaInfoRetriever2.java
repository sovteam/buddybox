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

import buddybox.core.Album;
import buddybox.core.Artist;
import buddybox.core.Playable;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.events.AlbumArtEmbeddedFound;
import buddybox.core.events.AlbumArtRequested;
import buddybox.web.DownloadUtils;
import buddybox.web.HttpUtils;

import static buddybox.core.Dispatcher.dispatch;

public class MediaInfoRetriever2 {

    private static final long ONE_MINUTE = 1000 * 60;
    private static final long ONE_MONTH  = ONE_MINUTE * 60 * 24 * 30;
    private static final String TAG = MediaInfoRetriever2.class.getSimpleName();

    private static final String ASSETS_FOLDER_PATH = Environment.DIRECTORY_MUSIC + File.separator + "Assets";
    private static final File ALBUMS_FOLDER  = produceAssetFolder(ASSETS_FOLDER_PATH + File.separator + "Albums");
    private static final File ARTISTS_FOLDER = produceAssetFolder(ASSETS_FOLDER_PATH + File.separator + "Artists");

    private static final String API_KEY = "c65adb3fdfa66e16cb4308ad76f2a052";
    private static Application context;

    private static long lastDownloadError;

    public static void init(Application context) {
        MediaInfoRetriever2.context = context;
    }

    public static Bitmap load(Playable playable) {
        if (playable instanceof Song)     return load((Song)     playable);
        if (playable instanceof Album)    return load((Album)    playable);
        if (playable instanceof Artist)   return load((Artist)   playable);
        if (playable instanceof Playlist) return load((Playlist) playable);
        Log.e(TAG, "Unknown Playable class: " + playable.getClass());
        return null;
    }

    private static Bitmap load(Song song) {
        Bitmap result = loadEmbeddedArt(song);
        return (result != null) ? result : loadExternalArt(song);
    }

    private static Bitmap load(Artist   artist)   { Log.d(TAG, "TODO: Load art for artist: "   + artist);   return null; }
    private static Bitmap load(Album    album)    { Log.d(TAG, "TODO: Load art for album: "    + album);    return null; }
    private static Bitmap load(Playlist playlist) { Log.d(TAG, "TODO: Load art for playlist: " + playlist); return null; }

    private static Bitmap loadEmbeddedArt(Song song) {
        if (song.hasEmbeddedArt() == Boolean.FALSE)
            return null;
        Bitmap result = getEmbeddedBitmap(song);
        if (song.hasEmbeddedArt() == null) {
            boolean wasFound = result != null;
            dispatch(new AlbumArtEmbeddedFound(song, wasFound));
        }
        return result;
    }

    private static Bitmap loadExternalArt(Song song) {
        Bitmap result = loadAlbumArtFromFolder(song);
        return (result != null) ? result : loadAlbumArtFromLastFM(song); //TODO: Bring file caching logic here.
    }

    private static Bitmap loadAlbumArtFromFolder(Song song) {
        File file = new File(ALBUMS_FOLDER, albumArtFileName(song.artist, song.album));
        return file.exists()
                ? BitmapFactory.decodeFile(file.getPath())
                : null;
    }

    private static Bitmap loadAlbumArtFromLastFM(Song song) {
        System.out.println(">>> loadFromLastFM: " + song.artist + "::" + song.album);
        if (System.currentTimeMillis() - song.lastAlbumArtRequested < ONE_MONTH)
            return null;
        if (System.currentTimeMillis() - lastDownloadError < ONE_MINUTE)
            return null;
        if (!Network.hasConnection(context))
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
            System.out.println(">>> AlbumArt ERROR: " + song.album + e.getMessage());
            return null;
        }

        dispatch(new AlbumArtRequested(song.artist, song.album));
        return art;
    }

    private static String albumArtFileName(String artist, String album) {
        return sanitize(artist + "-" + album);
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
                throw new IllegalStateException("Unable to create folder: " + folder);
        return folder;
    }

    private static String sanitize(String fileNameCandidate) {
        return fileNameCandidate
                .replaceAll("[^a-zA-Z0-9-_.]", "_")
                .toLowerCase();
    }

}
