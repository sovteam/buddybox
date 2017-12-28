package buddybox.impl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;

import buddybox.api.Core;
import buddybox.api.Play;
import buddybox.api.Playlist;
import buddybox.api.Song;
import buddybox.api.VisibleState;

import static buddybox.api.Play.*;

public class CoreImpl implements Core {

    private final Context context;
    private final MediaPlayer player;
    private final Handler handler = new Handler();

    private StateListener listener;
    private File musicDirectory;
    private int currentSongIndex;
    private Playlist recentPlaylist;

    public CoreImpl(Context context) {
        this.context = context;

        musicDirectory = this.context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        System.out.println(musicDirectory);

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { @Override public void onCompletion(MediaPlayer mediaPlayer) {
            currentSongIndex = recentPlaylist().songAfter(currentSongIndex, 1);
            play(recentPlaylist(), currentSongIndex);
            updateListener();
        }});
    }


    @Override
    public void dispatch(Event event) {
        if (event == PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event == SKIP_NEXT) skip(+1);
        if (event == SKIP_PREVIOUS) skip(-1);
        if (event.getClass() == Play.class) play((Play)event);
        updateListener();
    }

    private void skip(int step) {
        currentSongIndex = recentPlaylist.songAfter(currentSongIndex, step);
        play(recentPlaylist, currentSongIndex);
    }

    private void play(Play event) {
        play(event.playlist, event.songIndex);
    }

    private void play(Playlist playlist, int songIndex) {
        try {
            Uri myUri = Uri.parse(playlist.song(songIndex).file.getCanonicalPath());
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
            VisibleState state = new VisibleState(1, 1, null, recentPlaylist().song(currentSongIndex), null, !player.isPlaying(), null, null, null, recentPlaylist(), null, null);
            listener.update(state);
        }};
        handler.post(runnable);
    }

    private Playlist recentPlaylist() {
        if (recentPlaylist == null) {
            File[] files = musicDirectory.listFiles();
            ArrayList<Song> songs = new ArrayList<>();
            int nextId = 0;
            for (File file : files) {
                if (!file.getName().toLowerCase().endsWith(".mp3"))
                    continue;
                int id = nextId++;
                Song song = readSongMetadata(id, file);
                songs.add(song);
            }
            recentPlaylist = new Playlist(0, "Recent", songs);
        }
        return recentPlaylist;
    }

    @NonNull
    private SongImpl readSongMetadata(int id, File file) {
        return new SongImpl(id,
                file.getName().substring(0, file.getName().length() - 4),
                "Unknown Artist",
                "Unknown Genre",
                file);
    }

    @Override
    public void setStateListener(StateListener listener) {
        this.listener = listener;
        updateListener();
    }
}
