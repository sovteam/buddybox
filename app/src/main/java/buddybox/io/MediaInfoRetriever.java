package buddybox.io;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.LongSparseArray;

import com.adalbertosoares.buddybox.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.AlbumArtEmbeddedFound;
import buddybox.core.events.AlbumArtFound;
import buddybox.core.events.ArtistPictureFound;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.removeStateListener;

public class MediaInfoRetriever extends Service {

    private static final String API_KEY = "c65adb3fdfa66e16cb4308ad76f2a052";
    public static String ASSETS_FOLDER_PATH = Environment.DIRECTORY_MUSIC + File.separator + "Assets";
    public static String ALBUMS_FOLDER_PATH = ASSETS_FOLDER_PATH + File.separator + "Albums";
    public static String ARTISTS_FOLDER_PATH = ASSETS_FOLDER_PATH + File.separator + "Artists";

    private static Queue<AlbumArtRequest> albumQueue = new LinkedList<>();
    private static Boolean isConsumingAlbums = false;

    private Queue<Artist> artistsQueue = new LinkedList<>();
    private Boolean isConsumingArtists = false;
    private Bitmap defaultArtistPicture;

    private IModel.StateListener stateListener;
    private LongSparseArray<ImageRequest> pendingDownloads = new LongSparseArray<>();

    Map<String,Bitmap> albumsArt = new HashMap<>();
    Set<String> artistsRequested = new HashSet<>();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null)
                    return;

                Long id = extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
                ImageRequest request = pendingDownloads.get(id);
                if (request != null) {
                    Bitmap image = BitmapFactory.decodeFile(request.getFileName());
                    if (request.getClass() == AlbumArtRequest.class) {
                        // album art download
                        AlbumArtRequest albumRequest = (AlbumArtRequest) request;
                        dispatch(new AlbumArtFound(albumRequest.artist, albumRequest.albumName, image));
                    } else {
                        // artist picture download
                        dispatch(new ArtistPictureFound(((ArtistPictureRequest)request).artist, image));
                    }
                    pendingDownloads.remove(id);
                }
            }
        }
    };

    public MediaInfoRetriever() {}

    public static void init(Context context) {
        Intent intent = new Intent(context, MediaInfoRetriever.class);
        context.startService(intent);
    }

    public static File getAlbumsFolder() {
        return getAssetFolder(ALBUMS_FOLDER_PATH);
    }

    public static File getArtistsFolder() {
        return getAssetFolder(ARTISTS_FOLDER_PATH);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, filter);

        stateListener = new IModel.StateListener() {
            @Override
            public void update(State state) {
                updateState(state);
            }
        };
        addStateListener(stateListener);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        removeStateListener(stateListener);
        super.onDestroy();
    }

    interface ImageRequest {
        String getFileName();
    }

    private class ArtistPictureRequest implements ImageRequest {
        public final Artist artist;

        ArtistPictureRequest(Artist artist) {
            this.artist = artist;
        }
        @Override
        public boolean equals(Object obj) {
            return obj != null
                    && obj.getClass() == AlbumArtRequest.class
                    && ((ArtistPictureRequest) obj).artist.name.equals(artist.name);
        }

        public String getFileName() {
            return artistPictureFileName(artist);
        }
    }

    private class AlbumArtRequest implements ImageRequest {
        private final Artist artist;
        private final String albumName;

        AlbumArtRequest(Artist artist, String albumName) {
            this.artist = artist;
            this.albumName = albumName;
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null
                    && obj.getClass() == AlbumArtRequest.class
                    && ((AlbumArtRequest) obj).artist.name.equals(artist.name)
                    && ((AlbumArtRequest) obj).albumName.equals(albumName);
        }

        public String getFileName() {
            return albumArtFullFileName(artist.name, albumName);
        }
    }

    synchronized
    private void updateState(State state) {

        for (Artist artist : state.artists) {
            // TODO refactor
            if (artistsRequested.contains(artist.name))
                continue;
            artistsRequested.add(artist.name);

            for (String album : artist.songsByAlbum().keySet()) {
                boolean missingEmbedded = false;
                for (Song song : artist.songsByAlbum().get(album)) {
                    if (song.getArt() != null)
                        continue;
                    Bitmap art = getEmbeddedBitmap(song);
                    if (art != null)
                        dispatch(new AlbumArtEmbeddedFound(song, art));
                    else
                        missingEmbedded = true;
                }
                if (missingEmbedded)
                    findLocalAlbumArtOrEnqueueDownload(artist, album);
            }
            if (artist.picture == null)
                findLocalArtistPictureOrEnqueueDownload(artist);
        }
        consumeAlbumQueue();
        consumeArtistQueue();
    }

    private void findLocalArtistPictureOrEnqueueDownload(Artist artist) {
        Bitmap pic = getArtistPictureFromAssetsFolder(artist);
        if (pic != null) {
            dispatch(new ArtistPictureFound(artist, pic));
        } else if (!artistsQueue.contains(artist)) {
            artistsQueue.offer(artist);
        }
    }

    private void findLocalAlbumArtOrEnqueueDownload(Artist artist, String album) {
        String albumKey = albumArtFileName(artist.name, album);
        Bitmap art = getAlbumArtBitmap(albumKey);
        if (art != null) {
            dispatch(new AlbumArtFound(artist, album, art));
        } else {
            AlbumArtRequest request = new AlbumArtRequest(artist, album);
            if (!albumQueue.contains(request))
                albumQueue.offer(request);
        }
    }

    private Bitmap getAlbumArtBitmap(final String albumKey) {
        if (albumsArt.containsKey(albumKey))
            return albumsArt.get(albumKey);

        File fileFolder = MediaInfoRetriever.getAlbumsFolder();
        File[] images = fileFolder.listFiles(new FilenameFilter() { @Override public boolean accept(File dir, String name) {
            return name.equals(albumKey + ".png") || name.equals(albumKey + ".jpg") || name.equals(albumKey + ".jpeg");
        }});

        if (images.length > 0) {
            Bitmap art = BitmapFactory.decodeFile(images[0].getPath());
            albumsArt.put(albumKey, art);
        } else {
            albumsArt.put(albumKey, null);
        }

        return albumsArt.get(albumKey);
    }

    private Bitmap getArtistPictureFromAssetsFolder(final Artist artist) {
        File fileFolder = MediaInfoRetriever.getArtistsFolder();
        File[] images = fileFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
            return name.startsWith(artistPictureFileName(artist));
            }
        });
        if (images.length == 0)
            return null;

        return BitmapFactory.decodeFile(images[0].getPath());
    }

    private void consumeArtistQueue() {
        if (isConsumingArtists || artistsQueue.isEmpty() || !Network.hasConnection(this)) // TODO send connection to Model
            return;

        isConsumingArtists = true;
        Thread thread = new Thread(new Runnable() { @Override public void run() {
            consumeNextArtist();
        }});
        thread.start();
    }

    private void consumeNextArtist() {
        if (artistsQueue.isEmpty()) {
            isConsumingArtists = false;
            return;
        }

        Artist artist = artistsQueue.peek();
        retrieveArtistPicture(artist);

        // breath between API calls
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
            isConsumingArtists = false;
            return;
        }

        artistsQueue.remove();
        consumeNextArtist();
    }

    private void retrieveArtistPicture(Artist artist) {
        String artistName = Uri.encode(artist.name);
        final String urlString = "https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + "&format=json";

        JSONObject response = getHttpResponse(urlString);
        if (response == null) {
            dispatch(new ArtistPictureFound(artist, getDefaultArtistPicture()));
            return;
        }

        String artistPictureUrl = getArtistPictureUrl(response);
        System.out.println(">>> artistPictureUrl: " + artist.name);
        if (artistPictureUrl == null || artistPictureUrl.isEmpty()) {
            dispatch(new ArtistPictureFound(artist, getDefaultArtistPicture()));
            return;
        }

        ArtistPictureRequest request = new ArtistPictureRequest(artist);
        downloadImage(artistPictureUrl, ARTISTS_FOLDER_PATH, request);
    }

    private String artistPictureFullFileName(Artist artist) {
        return artistPictureFileName(artist) + ".png";
    }

    private String artistPictureFileName(Artist artist) {
        return encode(artist.name).toLowerCase();
    }

    private String getArtistPictureUrl(JSONObject json) {
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
                Log.d("MediaInfoRetriever", "JSON image empty");
                return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    public static Bitmap getEmbeddedBitmap(Song song) {
        byte[] picture = getEmbeddedPicture(song);

        if (picture == null)
            return null;

        return BitmapFactory.decodeByteArray(picture, 0, picture.length);
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

    private void consumeAlbumQueue() {
        if (isConsumingAlbums || albumQueue.isEmpty() || !Network.hasConnection(this)) // TODO send connection to Model
            return;

        isConsumingAlbums = true;
        Thread thread = new Thread(new Runnable() { @Override public void run() {
            consumeNextAlbum();
        }});
        thread.start();
    }

    private void consumeNextAlbum() {
        if (albumQueue.isEmpty()) {
            isConsumingAlbums = false;
            return;
        }

        AlbumArtRequest request = albumQueue.peek();
        retrieveAlbumArt(request);

        // breath between API calls
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
            isConsumingAlbums = false;
            return;
        }

        albumQueue.remove();
        consumeNextAlbum();
    }

    private void retrieveAlbumArt(AlbumArtRequest request) {
        String artist = Uri.encode(request.artist.name);
        final String urlString = "https://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist=" + artist + "&autocorrect=1&api_key=" + API_KEY + "&format=json";

        JSONObject response = getHttpResponse(urlString);
        if (response == null) {
            dispatch(new AlbumArtFound(request.artist, request.albumName, getDefaultArtistPicture()));
            return;
        }

        String imageUrl = getAlbumImageUrl(response);
        if (imageUrl == null) {
            dispatch(new AlbumArtFound(request.artist, request.albumName, getDefaultArtistPicture()));
            return;
        }

        downloadImage(imageUrl, ALBUMS_FOLDER_PATH, request);
    }

    private JSONObject getHttpResponse(String urlString) {
        // build URL
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        // open connection
        int responseCode;
        HttpURLConnection urlConnection;
        InputStream result;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Buddy Box App");
            responseCode = urlConnection.getResponseCode();
            result = urlConnection.getInputStream();
            // check response code
            if (responseCode != 200) {
                Log.d("MediaInfoRetriever", "RESPONSE CODE " + responseCode);
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject ret = buildJSON(result);
        urlConnection.disconnect();
        return ret;
    }

    private JSONObject buildJSON(InputStream inputStream) {
        StringBuilder builder;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        JSONObject ret;
        try {
            ret = new JSONObject(builder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    private String getAlbumImageUrl(JSONObject json) {
        // fetch response
        String ret;
        try {
            if (!json.has("topalbums"))
                return null;
            JSONObject top = json.getJSONObject("topalbums");

            if (!top.has("album"))
                return null;
            JSONArray albums = top.getJSONArray("album");

            if (albums.length() == 0)
                return null;
            JSONObject album = albums.getJSONObject(0);

            if (album != null) {
                JSONArray images = album.getJSONArray("image");
                if (images.length() != 0) {
                    // get largest image
                    JSONObject largestImage = images.getJSONObject(images.length() - 1); // TODO select image by size properly
                    ret = largestImage.getString("#text");
                } else {
                    Log.d("MediaInfoRetriever", "JSON image empty");
                    return null;
                }
            } else {
                Log.d("MediaInfoRetriever", "JSON album empty");
                return  null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    public static String albumArtFileName(Song song) {
        return encode(song.artist + "-" + song.album).toLowerCase();
    }

    public static String albumArtFileName(String artist, String album) {
        return encode(artist + "-" + album).toLowerCase();
    }

    public static String albumArtFullFileName(String artist, String album) {
        return albumArtFileName(artist, album) + ".png";
    }

    private static File getAssetFolder(String folderPath) {
        File folder = Environment.getExternalStoragePublicDirectory(folderPath);
        if (!folder.exists())
            if (!folder.mkdirs())
                Log.d("MediaInfoRetriever", "Unable to create folder: " + folder);
        return folder;
    }

    void downloadImage(String url, String folder, ImageRequest imageRequest) {
        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request downloadRequest = new DownloadManager.Request(downloadUri);

        downloadRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI) // TODO enable according to user preferences  | DownloadManager.Request.NETWORK_MOBILE
                .setAllowedOverRoaming(false)
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalPublicDir(folder, imageRequest.getFileName());

        DownloadManager man = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        assert man != null;
        long id = man.enqueue(downloadRequest);
        pendingDownloads.put(id, imageRequest);
    }

    public static String encode(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9-_\\.]", "_").toLowerCase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public Bitmap getDefaultArtistPicture() {
        if (defaultArtistPicture == null)
            defaultArtistPicture = BitmapFactory.decodeResource(getBaseContext().getResources(), R.mipmap.sneer2);
        return defaultArtistPicture;
    }
}
