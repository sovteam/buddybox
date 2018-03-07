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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.DownloadCompleted;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.removeStateListener;

public class MediaInfoRetriever extends Service {

    public static String ALBUMS_FOLDER_PATH = Environment.DIRECTORY_MUSIC + File.separator + "Albums";

    private static Queue<Song> queue = new LinkedList<>();
    private static Boolean isConsuming = false;

    private IModel.StateListener stateListener;
    private LongSparseArray<String> pendingDownloads = new LongSparseArray<>();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null)
                    return;

                Long id = extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
                String fileName = pendingDownloads.get(id);
                if (fileName != null) {
                    dispatch(new DownloadCompleted(fileName));
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(receiver, filter);

        initAlbumsFolder();

        stateListener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
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
        for (Song song : state.allSongsPlaylist.songs)
            if (shouldRetrieveMediaInfo(song))
                queue.offer(song);
        System.out.println(">> >> >> consume!!!!");
        consumeQueue();
    }

    private boolean shouldRetrieveMediaInfo(Song song) {
        return !song.hasRetrievedMediaInfo && !hasEmbeddedPicture(song) && !hasLocalAlbumArtFile(song);
    }

    public static Bitmap getEmbeddedBitmap(Song song) {
        byte[] picture = getEmbeddedPicture(song);

        if (picture == null)
            return null;

        return BitmapFactory.decodeByteArray(picture, 0, picture.length);
    }

    private static byte[] getEmbeddedPicture(Song song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(song.filePath);

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

    private boolean hasEmbeddedPicture(Song song) {
        return getEmbeddedPicture(song) != null;
    }

    private boolean hasLocalAlbumArtFile(Song song) {
        File dir = Environment.getExternalStoragePublicDirectory(ALBUMS_FOLDER_PATH);
        File image = new File(dir.getAbsolutePath(), imageFileName(song));
        System.out.println(">>> >>> >>> dir.getAbsolutePath: " + dir.getAbsolutePath() + ", file: " + imageFileName(song) + ", exists: " + image.exists());
        return image.exists();
    }

    private void consumeQueue() {
        System.out.println("!!!!!!!! CONSUME QUEUE, isConsuming: " + isConsuming + ", EMPTY: " + queue.isEmpty() + ", NETWORK HAS CONNECTION: " + Network.hasConnection(this));
        if (isConsuming || queue.isEmpty() || !Network.hasConnection(this)) // TODO send connection to Model
            return;

        isConsuming = true;
        Thread thread = new Thread(new Runnable() { @Override public void run() {
            consumeNext();
        }});
        thread.start();
    }

    private void consumeNext() {
        if (queue.isEmpty()) {
            System.out.println("!!!!!!!! CONSUME QUEUE EMPTY");
            isConsuming = false;
            return;
        }

        Song song = queue.peek();
        if (shouldRetrieveMediaInfo(song)) {
            retrieveMediaInfo(song);
            song.setHasRetrievedMediaInfo();

            // breath between API calls
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
                isConsuming = false;
                return;
            }
        }
        queue.remove();
        consumeNext();
    }

    private void retrieveMediaInfo(Song song) {
        String artist = Uri.encode(song.artist);
        final String urlString = "https://ws.audioscrobbler.com/2.0/?method=artist.gettopalbums&artist=" + artist + "&autocorrect=1&api_key=c65adb3fdfa66e16cb4308ad76f2a052&format=json";

        JSONObject response = getHttpResponse(urlString);
        if (response == null)
            return;

        String imageUrl = getImageUrl(response);
        if (imageUrl == null)
            return;

        downloadImage(imageUrl, imageFileName(song));
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

    public static String fileNamePattern(Song song) {
        return encode(song.artist + "-" + song.album).toLowerCase();
    }

    public static String imageFileName(Song song) {
        return fileNamePattern(song) + ".png";
    }

    private void initAlbumsFolder() {
        File albumsFolder = Environment.getExternalStoragePublicDirectory(ALBUMS_FOLDER_PATH);
        if (!albumsFolder.exists())
            if (!albumsFolder.mkdirs())
                Log.d("MediaInfoRetriever", "Unable to create album folder: " + albumsFolder);
    }

    void downloadImage(String url, String fileName) {
        Uri downloadUri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI) // TODO enable according to user preferences  | DownloadManager.Request.NETWORK_MOBILE
                .setAllowedOverRoaming(false)
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalPublicDir(ALBUMS_FOLDER_PATH, fileName);

        DownloadManager man = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        assert man != null;
        long id = man.enqueue(request);
        pendingDownloads.put(id, fileName);
    }

    public static String encode(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9-_\\.]", "_").toLowerCase();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
