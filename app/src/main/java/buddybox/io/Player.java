package buddybox.io;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;

import static buddybox.core.events.Play.FINISHED_PLAYING;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.dispatch;

public class Player {
    private static MediaPlayer mediaPlayer;
    private static Context context;
    private static Song songPlaying;

    private static Handler handler = new Handler();
    private static boolean playCycleRunning = false;
    private static List<ProgressListener> listeners = new ArrayList<>();


    public static void init(Context context) {
        Player.context = context;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { @Override public void onCompletion(MediaPlayer mediaPlayer) {
            dispatch(FINISHED_PLAYING);
        }});

        addStateListener(new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }});
    }

    private static void updateState(State state) {
        if (state.seekTo != null) {
            mediaPlayer.seekTo(state.seekTo);
            return;
        }

        if (state.songPlaying == null || state.isPaused || state.songPlaying.isMissing) {
            if (songPlaying != null) {
                mediaPlayer.pause();
            }
            return;
        }


        if (songPlaying == state.songPlaying) {
            mediaPlayer.start();
            startPlayCycle();
            return;
        }

        try {
            Uri myUri = Uri.parse(state.songPlaying.filePath);
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, myUri);
            mediaPlayer.prepare();
            mediaPlayer.start();
            startPlayCycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        songPlaying = state.songPlaying;
    }

    private static void startPlayCycle() {
        if (!playCycleRunning) {
            playCycleRunning = true;
            playCycle();
        }
    }

    public static void removeListener(ProgressListener progressListener) {
        listeners.remove(progressListener);
    }

    public interface ProgressListener {
        void updateProgress(int progress);
    }

    public static void addListener(ProgressListener listener) {
        listeners.add(listener);
        listener.updateProgress(mediaPlayer.getCurrentPosition());
    }

    private static void playCycle() {
        notifyListeners();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
            if (mediaPlayer.isPlaying())
                playCycle();
            else
                playCycleRunning = false;
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    private static void notifyListeners() {
        System.out.println("update listeners: " + mediaPlayer.getCurrentPosition());
        for (ProgressListener listener : listeners) {
            listener.updateProgress(mediaPlayer.getCurrentPosition());
        }
    }
}
