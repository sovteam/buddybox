package buddybox.io;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
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
import java.util.LinkedList;
import java.util.Queue;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.AlbumArtDownloadCompleted;
import buddybox.core.events.ArtistPictureDownloadCompleted;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.removeStateListener;

public class MediaInfoRetriever extends Service {

    private static final String API_KEY = "c65adb3fdfa66e16cb4308ad76f2a052";
    public static String ASSETS_FOLDER_PATH = Environment.DIRECTORY_MUSIC + File.separator + "Assets";
    public static String ALBUMS_FOLDER_PATH = ASSETS_FOLDER_PATH + File.separator + "Albums";
    public static String ARTISTS_FOLDER_PATH = ASSETS_FOLDER_PATH + File.separator + "Artists";

    private static Queue<Song> songsQueue = new LinkedList<>();
    private static Boolean isConsumingSongs = false;

    private Queue<Artist> artistsQueue = new LinkedList<>();
    private Boolean isConsumingArtists = false;
    private Bitmap defaultArtistPicture;

    private IModel.StateListener stateListener;
    private LongSparseArray<ImageDownload> pendingDownloads = new LongSparseArray<>();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null)
                    return;

                Long id = extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
                ImageDownload download = pendingDownloads.get(id);
                if (download != null) {
                    if (download.getClass() == ArtistPictureDownload.class) {
                        // artist pic download
                        dispatch(new ArtistPictureDownloadCompleted(download.fileName, ((ArtistPictureDownload)download).artist));
                    } else {
                        // album art download
                        dispatch(new AlbumArtDownloadCompleted(download.fileName));
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

    public static File getAtistsFolder() {
        return getAssetFolder(ARTISTS_FOLDER_PATH);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        removeStateListener(stateListener);
        super.onDestroy();
    }

    synchronized
    private void updateState(State state) {
        // artist picture queue
        for (Artist artist : state.artists) {
            if (artist.picture == null) {
                Bitmap art = getArtistPictureFromLocalFolder(artist);
                if (art != null) {
                    artist.setPicture(art);
                } else {
                    artist.setPicture(getDefaultArtistPicture());
                    artistsQueue.offer(artist);
                }
            }
        }
        consumeArtistQueue();

        // album art queue
        for (Song song : state.allSongsPlaylist.songs)
            if (song.getArt() == null) {
                song.setArt(getDefaultArtistPicture());
                songsQueue.offer(song);
            }
        consumeSongsQueue();
    }

    private Bitmap getArtistPictureFromLocalFolder(final Artist artist) {
        File fileFolder = MediaInfoRetriever.getAtistsFolder();
        File[] images = fileFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.equals(artistPictureFullFileName(artist));
            }
        });
        if (images.length == 0)
            return null;

        return BitmapFactory.decodeFile(images[0].getAbsolutePath());
    }

    private void consumeArtistQueue() {
        System.out.println(">> >> MediaInfoRetriever: CONSUME ARTISTS QUEUE, isConsumingArtists: " + isConsumingArtists + ", EMPTY: " + artistsQueue.isEmpty() + ", NETWORK HAS CONNECTION: " + Network.hasConnection(this));
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
            System.out.println(">> MediaInfoRetriever: Artists QUEUE EMPTY");
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
        if (response == null)
            return;

        String artistPictureUrl = getArtistPictureUrl(response);
        if (artistPictureUrl == null)
            return;

        ArtistPictureDownload download = new ArtistPictureDownload(artistPictureFullFileName(artist), artist);
        downloadImage(artistPictureUrl, ARTISTS_FOLDER_PATH, download);
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
            JSONObject artist = json.getJSONObject("artist");
            if (artist == null)
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

    private void consumeSongsQueue() {
        System.out.println(">> >> CONSUME QUEUE, isConsumingSongs: " + isConsumingSongs + ", EMPTY: " + songsQueue.isEmpty() + ", NETWORK HAS CONNECTION: " + Network.hasConnection(this));
        if (isConsumingSongs || songsQueue.isEmpty() || !Network.hasConnection(this)) // TODO send connection to Model
            return;

        isConsumingSongs = true;
        Thread thread = new Thread(new Runnable() { @Override public void run() {
            consumeNextSong();
        }});
        thread.start();
    }

    private void consumeNextSong() {
        if (songsQueue.isEmpty()) {
            isConsumingSongs = false;
            return;
        }

        Song song = songsQueue.peek();

        if (!hasLocalAlbumArtFile(song)) {
            retrieveAlbumArt(song);

            // breath between API calls
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
                isConsumingSongs = false;
                return;
            }
        }
        songsQueue.remove();
        consumeNextSong();
    }

    private boolean hasLocalAlbumArtFile(Song song) {
        File dir = Environment.getExternalStoragePublicDirectory(ALBUMS_FOLDER_PATH);
        File image = new File(dir.getAbsolutePath(), albumArtFileName(song));
        return image.exists();
    }

    private void retrieveAlbumArt(Song song) {
        String artist = Uri.encode(song.artist);
        final String urlString = "https://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist=" + artist + "&autocorrect=1&api_key=" + API_KEY + "&format=json";

        JSONObject response = getHttpResponse(urlString);
        if (response == null)
            return;

        String imageUrl = getImageUrl(response);
        if (imageUrl == null)
            return;

        downloadImage(imageUrl, ALBUMS_FOLDER_PATH, new ImageDownload(albumArtFullFileName(song)));
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

    private String getImageUrl(JSONObject json) {
        // fetch response
        String ret;
        try {
            JSONObject top = json.getJSONObject("topalbums");
            JSONArray albums = top.getJSONArray("album");
            JSONObject album = albums.getJSONObject(0);
            if (album != null) {
                JSONArray images = album.getJSONArray("image");
                if (images.length() != 0) {
                    // get largest image
                    JSONObject largestImage = images.getJSONObject(images.length() - 1); // TODO select image size properly
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

    public static String albumArtFullFileName(Song song) {
        return albumArtFileName(song) + ".png";
    }

    private static File getAssetFolder(String folderPath) {
        File folder = Environment.getExternalStoragePublicDirectory(folderPath);
        if (!folder.exists())
            if (!folder.mkdirs())
                Log.d("MediaInfoRetriever", "Unable to create folder: " + folder);
        return folder;
    }

    class ImageDownload {
        private final String fileName;

        private ImageDownload(String fileName) {
            this.fileName = fileName;
        }
    }

    class ArtistPictureDownload extends ImageDownload {
        private final Artist artist;

        private ArtistPictureDownload(String fileName, Artist artist){
            super(fileName);
            this.artist = artist;
        }
    }

    void downloadImage(String url, String folder, ImageDownload download) {
        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI) // TODO enable according to user preferences  | DownloadManager.Request.NETWORK_MOBILE
                .setAllowedOverRoaming(false)
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalPublicDir(folder, download.fileName);

        DownloadManager man = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        assert man != null;
        long id = man.enqueue(request);
        pendingDownloads.put(id, download);
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
        if (defaultArtistPicture == null) {
            AssetManager assetManager = getAssets();
            try {
                defaultArtistPicture = BitmapFactory.decodeStream(assetManager.open("sneer2.jpg"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return defaultArtistPicture;
    }
}
