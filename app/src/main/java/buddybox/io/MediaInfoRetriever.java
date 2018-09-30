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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.AlbumArtError;
import buddybox.core.events.AlbumArtFound;
import buddybox.core.events.AlbumArtNotFound;
import buddybox.core.events.ArtistInfoError;
import buddybox.core.events.ArtistInfoFound;
import buddybox.core.events.ArtistPictureFound;
import buddybox.model.AlbumInfo;
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

    private static Boolean isRunning = false;

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

    synchronized
    private static void updateState(final State state) {
        if (isRunning || !Network.hasConnection(context)) return;

        isRunning = true;
        service.submit(new Runnable() { @Override public void run() {
            try {
                updateStateInner(state);
            } finally {
                synchronized (MediaInfoRetriever.class) { isRunning = false; }
                // dispatch(new InfoRetrival());
            }
        }});
    }

    private static void updateStateInner(State state) {
        // find album art
        AlbumInfo album = state.albumToFindArt;
        if (album != null)
            consumeAlbum(album);

        // find artist info
        Artist artist = state.artistToFindInfo;
        if (artist != null)
            consumeArtist(artist);
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

    private static void consumeArtist(Artist artist) {
        if (artist.picture != null)
            return;

        Bitmap pic = getArtistPictureFromAssetsFolder(artist);
        if (pic != null) {
            dispatch(new ArtistPictureFound(artist, pic));
            return;
        }
        retrieveArtistPictureAndBio(artist);
    }

    private static void retrieveArtistPictureAndBio(Artist artist) {
        String artistName = Uri.encode(artist.name);
        final String urlString = "https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + "&format=json";

        Bitmap pic = null;
        String bio;
        try {
            JSONObject response = HttpUtils.getHttpResponse(urlString);
            String artistPictureUrl = getArtistPictureUrl(response);
            if (artistPictureUrl == null || artistPictureUrl.isEmpty()) {
                Log.d(TAG, "no picture url");
            } else {
                String picPath = getAssetFolder(ARTISTS_FOLDER_PATH) + File.separator + artistPictureFullFileName(artist);
                pic = downloadImage(artistPictureUrl, picPath);
            }

            bio = getArtistBio(response);
        } catch (Exception e) {
            System.out.println(">>> ERROR retrieveArtistPictureAndBio");
            dispatch(new ArtistInfoError(artist));
            return;
        }

        dispatch(new ArtistInfoFound(artist, pic, bio));
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

    private static void consumeAlbum(AlbumInfo albumInfo) {
        System.out.println(">>> consumeAlbum: " + albumInfo.name);
        String artist = Uri.encode(albumInfo.artist);
        String album = Uri.encode(albumInfo.name);
        String urlString = "https://ws.audioscrobbler.com/2.0/?method=album.getinfo&artist=" + artist + "&album=" + album + "&autocorrect=1&api_key=" + API_KEY + "&format=json";

        Bitmap art;
        try {
            JSONObject response = HttpUtils.getHttpResponse(urlString);
            String imageUrl = getAlbumImageUrl(response);
            if (imageUrl == null) {
                System.out.println(">>> consumeAlbum NOT FOUND: " + albumInfo.name);
                dispatch(new AlbumArtNotFound(albumInfo));
                return;
            }
            String fullPath = getAssetFolder(ALBUMS_FOLDER_PATH) + File.separator + albumArtFullFileName(albumInfo.artist, albumInfo.name);
            art = downloadImage(imageUrl, fullPath);
        } catch (Exception e) {
            System.out.println(">>> consumeAlbum ERROR: " + albumInfo.name);
            dispatch(new AlbumArtError(albumInfo));
            return;
        }

        System.out.println(">>> consumeAlbum FOUND: " + albumInfo.name);
        dispatch(new AlbumArtFound(albumInfo, art));
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

    private static Bitmap downloadImage(String urlStr, String fullPath) {
        // download image
        byte[] response = DownloadUtils.downloadFile(urlStr);
        if (response == null)
            return null;

        // save downloaded image
        try {
            FileOutputStream fos = new FileOutputStream(fullPath);
            fos.write(response);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DownloadUtils", "IOException writing file");
            return null;
        }

        return BitmapFactory.decodeFile(fullPath);
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
