package buddybox.impl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import buddybox.api.Core;
import buddybox.api.Play;
import buddybox.api.Playlist;
import buddybox.api.Song;
import buddybox.api.VisibleState;

import static buddybox.api.Play.PLAY_PAUSE_CURRENT;
import static buddybox.api.Play.SKIP_NEXT;
import static buddybox.api.Play.SKIP_PREVIOUS;

import static buddybox.api.Sampler.*;

public class CoreImpl implements Core {

    private final Context context;
    private final MediaPlayer player;
    private final Handler handler = new Handler();

    private StateListener listener;
    private File musicDirectory;

    private int currentSongIndex;
    private Playlist recentPlaylist;

    private boolean isSampling = false;
    private Song sampling;

    private int nextId;

    public CoreImpl(Context context) {
        this.context = context;

        musicDirectory = this.context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        System.out.println(">>> Music directory: " + musicDirectory);

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { @Override public void onCompletion(MediaPlayer mediaPlayer) {
            play(recentPlaylist(), recentPlaylist().songAfter(currentSongIndex, 1));
            updateListener();
        }});
    }


    @Override
    public void dispatch(Event event) {
        if (event == PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event == SKIP_NEXT) skip(+1);
        if (event == SKIP_PREVIOUS) skip(-1);
        if (event.getClass() == Play.class) play((Play)event);

        if (event == SAMPLER_START) samplerStart();
        if (event == SAMPLER_STOP) samplerStop();

        updateListener();
    }

    private void samplerStop() {
        if (isSampling)
            player.stop();
        isSampling = false;
    }

    private void samplerStart() {
        isSampling = true;
        sampling = recentPlaylist.songs.get(0);
        List<Song> songs = Collections.singletonList(sampling);
        play(new Playlist(666, "Sampling", songs), 0);
    }

    private void skip(int step) {
        play(recentPlaylist, recentPlaylist.songAfter(currentSongIndex, step));
    }

    private void play(Play event) {
        play(event.playlist, event.songIndex);
    }

    private void play(Playlist playlist, int songIndex) {
        // TODO set currentPlaylist
        currentSongIndex = songIndex;
        try {
            Uri myUri = Uri.parse(playlist.song(songIndex).file.getCanonicalPath());
            player.pause();
            player.reset();
            player.setDataSource(context, myUri);
            player.prepare();
            player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playPauseCurrent() {
        if (player.isPlaying())
            player.pause();
        else
            player.start();
    }


    private void updateListener() {
        Runnable runnable = new Runnable() { @Override public void run() {
            VisibleState state = new VisibleState(1, null, isSampling ? null : recentPlaylist().song(currentSongIndex), null, !player.isPlaying(), null, isSampling ? sampling : null, null, recentPlaylist(), null, null, 1);
            listener.update(state);
        }};
        handler.post(runnable);
    }

    private Playlist recentPlaylist() {
        if (recentPlaylist == null) {
            recentPlaylist = new Playlist(0, "Recent", listSongs(musicDirectory));
        }
        return recentPlaylist;
    }

    private ArrayList<Song> listSongs(File directory) {
        ArrayList<Song> songs = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                songs.addAll(listSongs(file));
            } else {
                if (!file.getName().toLowerCase().endsWith(".mp3"))
                    continue;
                int id = nextId();
                Song song = readSongMetadata(id, file);
                songs.add(song);
            }
        }
        return songs;
    }

    private int nextId() {
        return nextId++;
    }

    @NonNull
    private SongImpl readSongMetadata(int id, File file) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getPath());

        String name = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (name == null || name.trim().isEmpty())
            name = file.getName().substring(0, file.getName().length() - 4);

        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if (artist == null || artist.trim().isEmpty())
            artist = "Unknown Artist";

        String genre = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        if (genre == null || genre.trim().isEmpty())
            genre = "Unknown Genre";

        return new SongImpl(id, name, artist, genre, file);
    }

    @Override
    public void setStateListener(StateListener listener) {
        this.listener = listener;
        updateListener();
    }
}
