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
import java.net.MalformedURLException;
import java.net.URL;

import buddybox.core.Album;
import buddybox.core.Artist;
import buddybox.core.Playable;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.events.AlbumArtEmbeddedFound;
import buddybox.core.events.AlbumArtRequested;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.web.DownloadUtils.download;
import static buddybox.web.HttpUtils.getJson;

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

    public static Bitmap loadArt(Playable playable) {
        if (playable instanceof Album)    return loadArt((Album)    playable);
        if (playable instanceof Artist)   return loadArt((Artist)   playable);
        if (playable instanceof Playlist) return loadArt((Playlist) playable);
        if (playable instanceof Song)     return loadArt((Song)     playable);
        throw new IllegalArgumentException("Unknown Playable class: " + playable.getClass());
    }

    private static Bitmap loadArt(Song song) {
        try {
            Bitmap result = loadEmbeddedArt(song);
            return (result != null) ? result : loadExternalArt(song);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap loadArt(Album    album)    { Log.d(TAG, "TODO: Load art for album: "    + album);    return null; }
    private static Bitmap loadArt(Artist   artist)   { Log.d(TAG, "TODO: Load art for artist: "   + artist);   return null; }
    private static Bitmap loadArt(Playlist playlist) { Log.d(TAG, "TODO: Load art for playlist: " + playlist); return null; }

    private static Bitmap loadEmbeddedArt(Song song) {
        if (song.hasEmbeddedArt() == Boolean.FALSE)
            return null;
        byte[] result = getEmbeddedArtBytes(song);
        if (song.hasEmbeddedArt() == null) {
            boolean wasFound = result != null;
            dispatch(new AlbumArtEmbeddedFound(song, wasFound));
        }
        return result == null
            ? null
            : BitmapFactory.decodeByteArray(result, 0, result.length);
    }

    private static Bitmap loadExternalArt(Song song) throws IOException {
        File cache = new File(ALBUMS_FOLDER, albumArtFileName(song.artist, song.album));
        if (cache.exists())
            return BitmapFactory.decodeFile(cache.getPath());

        byte[] downloaded = downloadArtFromLastFM(song);
        if (downloaded == null)
            return null;

        try (FileOutputStream fos = new FileOutputStream(cache)) {
            fos.write(downloaded);
        }
        return BitmapFactory.decodeByteArray(downloaded, 0, downloaded.length);
    }

    private static byte[] downloadArtFromLastFM(Song song) {
        if (System.currentTimeMillis() - song.lastAlbumArtRequested < ONE_MONTH)
            return null;
        if (System.currentTimeMillis() - lastDownloadError < ONE_MINUTE)
            return null;
        if (!Network.hasConnection(context))
            return null;

        URL url;
        try {
            url = new URL("https://ws.audioscrobbler.com/2.0/?method=album.getinfo&format=json" +
                    "&artist=" + Uri.encode(song.artist) +
                    "&album="  + Uri.encode(song.album)  +
                    "&autocorrect=1" +
                    "&api_key=" + API_KEY);
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL for artist: " + song.artist + " album: " + song.album);
            return null;
        }

        System.out.println(">>> downloadArtFromLastFM: " + song.artist + "::" + song.album);
        byte[] result = null;
        try {
            String imageUrl = getAlbumImageUrl(getJson(url));
            if (imageUrl != null)
                result = download(imageUrl);
        } catch (IOException e) {
            System.out.println(">>> AlbumArt ERROR: " + song.album + e.getMessage());
            lastDownloadError = System.currentTimeMillis();
            return null;
        }

        System.out.println(">>> AlbumArt FOUND: " + song.album);
        dispatch(new AlbumArtRequested(song.artist, song.album));
        return result;
    }

    private static String albumArtFileName(String artist, String album) {
        return sanitize(artist + "-" + album);
    }

    private static String getAlbumImageUrl(JSONObject json) {
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
            String result = image.getString("#text").trim();
            return result.isEmpty() ? null : result;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] getEmbeddedArtBytes(Song song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(song.filePath);
            return retriever.getEmbeddedPicture();
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
