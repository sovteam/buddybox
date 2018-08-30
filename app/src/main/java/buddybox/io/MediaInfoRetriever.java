package buddybox.io;

import android.content.Context;
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.AlbumArtEmbeddedFound;
import buddybox.core.events.AlbumArtFound;
import buddybox.core.events.ArtistBioFound;
import buddybox.core.events.ArtistPictureFound;
import buddybox.web.DownloadUtils;
import buddybox.web.HttpUtils;
import sov.buddybox.R;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.ui.ModelProxy.addStateListener;

public class MediaInfoRetriever {

    private static String TAG = "MediaInfoRetriever";

    private static final String API_KEY = "c65adb3fdfa66e16cb4308ad76f2a052";
    private static String ASSETS_FOLDER_PATH = Environment.DIRECTORY_MUSIC + File.separator + "Assets";
    private static String ALBUMS_FOLDER_PATH = ASSETS_FOLDER_PATH + File.separator + "Albums";
    private static String ARTISTS_FOLDER_PATH = ASSETS_FOLDER_PATH + File.separator + "Artists";

    private static ExecutorService service = Executors.newCachedThreadPool();

    private static Queue<AlbumArtRequest> albumQueue = new LinkedList<>();
    private static Boolean isConsumingAlbums = false;

    private static Queue<Artist> artistsQueue = new LinkedList<>();
    private static Boolean isConsumingArtists = false;

    private static Context context;

    private static Bitmap defaultArtistPicture;

    public MediaInfoRetriever() {}

    public static void init(Context context) {
        MediaInfoRetriever.context = context;
        addStateListener(new IModel.StateListener() {@Override public void update(final State state) {
            updateState(state);
        }});
    }

    private static File getAlbumsFolder() {
        return getAssetFolder(ALBUMS_FOLDER_PATH);
    }

    private static File getArtistsFolder() {
        return getAssetFolder(ARTISTS_FOLDER_PATH);
    }

    interface ImageRequest {
        String getFileFullPath();

        void finish(Bitmap bitmap);
    }

    private static class ArtistPictureRequest implements ImageRequest {
        public final Artist artist;

        ArtistPictureRequest(Artist artist) {
            this.artist = artist;
        }

        @Override
        public void finish(Bitmap picture) {
            dispatch(new ArtistPictureFound(artist, picture));
        }

        public String getFileFullPath() {
            return getAssetFolder(ARTISTS_FOLDER_PATH) + File.separator + artistPictureFullFileName(artist);
        }
    }

    private static class AlbumArtRequest implements ImageRequest {
        private final String artistName;
        private final String albumName;

        AlbumArtRequest(String artist, String albumName) {
            this.artistName = artist;
            this.albumName = albumName;
        }

        @Override
        public void finish(Bitmap art) {
            dispatch(new AlbumArtFound(artistName, albumName, art));
        }

        public String getFileFullPath() {
            return getAssetFolder(ALBUMS_FOLDER_PATH) + File.separator + albumArtFullFileName(artistName, albumName);
        }
    }

    private static void updateState(final State state) {
        service.submit(new Runnable() { @Override public void run() {
            updateStateAsync(state);
        }});
    }

    private static synchronized void updateStateAsync(State state) {
        // collect artistAlbums of songs without embedded art
        Map<String,Set<String>> artistsAlbums = new HashMap<>();
        for (Song song : state.allSongs) {
            if (song.getArt() != null)
                continue;

            Bitmap art = getEmbeddedBitmap(song);
            if (art != null) {
                dispatch(new AlbumArtEmbeddedFound(song, art));
            } else {
                Set<String> albums = artistsAlbums.get(song.artist);
                if (albums == null)
                    albums = new HashSet<>();
                albums.add(song.album);
                artistsAlbums.put(song.artist, albums);
            }
        }

        for (String artistName : artistsAlbums.keySet())
            for (String albumName : artistsAlbums.get(artistName))
                findLocalAlbumArtOrEnqueueDownload(artistName, albumName);

        for (Artist artist : state.artists)
            if (artist.picture == null)
                findLocalArtistPictureOrEnqueueDownload(artist);

        consumeAlbumQueue();
        consumeArtistQueue();
    }

    private static void findLocalArtistPictureOrEnqueueDownload(Artist artist) {
        Bitmap pic = getArtistPictureFromAssetsFolder(artist);
        if (pic != null)
            dispatch(new ArtistPictureFound(artist, pic));
        else
            artistsQueue.offer(artist);
    }

    private static void findLocalAlbumArtOrEnqueueDownload(String artistName, String albumName) {
        String albumKey = albumArtFileName(artistName, albumName);
        Bitmap art = getAlbumArtBitmap(albumKey);
        if (art != null) {
            dispatch(new AlbumArtFound(artistName, albumName, art));
        } else {
            AlbumArtRequest request = new AlbumArtRequest(artistName, albumName);
            albumQueue.offer(request);
        }
    }

    private static Bitmap getAlbumArtBitmap(final String albumKey) {
        File fileFolder = getAlbumsFolder();
        File[] images = fileFolder.listFiles(new FilenameFilter() { @Override public boolean accept(File dir, String name) {
            return name.equals(albumKey + ".png") || name.equals(albumKey + ".jpg") || name.equals(albumKey + ".jpeg");
        }});

        if (images.length > 0) {
            return BitmapFactory.decodeFile(images[0].getPath());
        }

        return null;
    }

    private static Bitmap getArtistPictureFromAssetsFolder(final Artist artist) {
        File fileFolder = getArtistsFolder();
        File[] images = fileFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
            return name.startsWith(artistPictureFileName(artist));
            }
        });
        if (images.length == 0)
            return null;

        return BitmapFactory.decodeFile(images[0].getPath());
    }

    private static void consumeArtistQueue() {
        if (isConsumingArtists || artistsQueue.isEmpty() || !Network.hasConnection(context)) // TODO send connection to Model
            return;

        isConsumingArtists = true;
        consumeNextArtist();
    }

    private static void consumeNextArtist() {
        if (artistsQueue.isEmpty()) {
            isConsumingArtists = false;
            return;
        }

        Artist artist = artistsQueue.peek();
        retrieveArtistPicture(artist);

        // breath between API calls
        Thread.yield();

        artistsQueue.remove();
        consumeNextArtist();
    }

    private static void retrieveArtistPicture(Artist artist) {
        String artistName = Uri.encode(artist.name);
        final String urlString = "https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + "&format=json";

        JSONObject response = HttpUtils.getHttpResponse(urlString);
        if (response == null) {
            Log.d(TAG, "no http response");
            dispatch(new ArtistPictureFound(artist, getDefaultImage()));
            return;
        }

        String bio = getArtistBio(response);
        if (bio != null)
            dispatch(new ArtistBioFound(artist, bio));

        String artistPictureUrl = getArtistPictureUrl(response);
        if (artistPictureUrl == null || artistPictureUrl.isEmpty()) {
            Log.d(TAG, "no picture url");
            dispatch(new ArtistPictureFound(artist, getDefaultImage()));
            return;
        }

        downloadImage(artistPictureUrl, new ArtistPictureRequest(artist));
    }

    private static String artistPictureFullFileName(Artist artist) {
        return artistPictureFileName(artist) + ".png";
    }

    private static String artistPictureFileName(Artist artist) {
        return encode(artist.name).toLowerCase();
    }

    private static String getArtistPictureUrl(JSONObject json) {
        // fetch response
        String ret;
        try {
            if (!json.has("artist"))
                return null;
            JSONObject artist = json.getJSONObject("artist");

            if (!artist.has("image"))
                return null;
            JSONArray images = artist.getJSONArray("image");

            if (images.length() != 0) {
                JSONObject picture = images.getJSONObject(images.length() -1); // TODO select image by size
                ret = picture.getString("#text");
            } else {
                Log.d(TAG, "JSON image empty");
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    private static String getArtistBio(JSONObject json) {
        String ret = "";
        try {
            if (!json.has("artist"))
                return null;
            JSONObject artist = json.getJSONObject("artist");
            if (!artist.has("bio"))
                return null;
            JSONObject bio = artist.getJSONObject("bio");
            Log.i(TAG, bio.toString());
            if (!bio.has("content"))
                return null;
            ret = bio.getString("content").trim();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (ret.isEmpty())
            return null;
        return ret;
    }

    private static Bitmap getEmbeddedBitmap(Song song) {
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

        byte[] ret;
        try {
            ret = retriever.getEmbeddedPicture();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            retriever.release();
        }

        return ret;
    }

    private static void consumeAlbumQueue() {
        if (isConsumingAlbums || albumQueue.isEmpty() || !Network.hasConnection(context)) // TODO send connection to Model
            return;

        isConsumingAlbums = true;
        consumeNextAlbum();
    }

    private static void consumeNextAlbum() {
        if (albumQueue.isEmpty()) {
            isConsumingAlbums = false;
            return;
        }

        AlbumArtRequest request = albumQueue.peek();
        retrieveAlbumArt(request);

        // breath between API calls
        Thread.yield();

        albumQueue.remove();
        consumeNextAlbum();
    }

    private static void retrieveAlbumArt(AlbumArtRequest request) {
        String artist = Uri.encode(request.artistName);
        String album = Uri.encode(request.albumName);
        final String urlString = "https://ws.audioscrobbler.com/2.0/?method=album.getinfo&artist=" + artist + "&album=" + album + "&autocorrect=1&api_key=" + API_KEY + "&format=json";

        JSONObject response = HttpUtils.getHttpResponse(urlString);
        if (response == null) {
            dispatch(new AlbumArtFound(request.artistName, request.albumName, getDefaultImage()));
            return;
        }

        String imageUrl = getAlbumImageUrl(response);
        if (imageUrl == null) {
            dispatch(new AlbumArtFound(request.artistName, request.albumName, getDefaultImage()));
            return;
        }

        downloadImage(imageUrl, request);
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

    private static String albumArtFileName(String artist, String album) {
        return encode(artist + "-" + album).toLowerCase();
    }

    private static String albumArtFullFileName(String artist, String album) {
        return albumArtFileName(artist, album) + ".png";
    }

    private static File getAssetFolder(String folderPath) {
        File folder = Environment.getExternalStoragePublicDirectory(folderPath);
        if (!folder.exists())
            if (!folder.mkdirs())
                Log.d(TAG, "Unable to create folder: " + folder);
        return folder;
    }

    private static void downloadImage(String urlStr, ImageRequest imageRequest) {
        // download image
        byte[] response = DownloadUtils.downloadFile(urlStr);
        if (response == null) {
            imageRequest.finish(getDefaultImage());
            return;
        }

        // save downloaded image
        try {
            FileOutputStream fos = new FileOutputStream(imageRequest.getFileFullPath());
            fos.write(response);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DownloadUtils", "IOException writing file");
            imageRequest.finish(getDefaultImage());
            return;
        }

        Bitmap image = BitmapFactory.decodeFile(imageRequest.getFileFullPath());
        imageRequest.finish(image);
    }

    private static String encode(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9-_\\.]", "_").toLowerCase();
    }

    private static Bitmap getDefaultImage() {
        if (defaultArtistPicture == null)
            defaultArtistPicture = BitmapFactory.decodeResource(context.getResources(), R.mipmap.sneer2);
        return defaultArtistPicture;
    }
}
