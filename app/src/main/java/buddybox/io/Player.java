package buddybox.io;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import buddybox.core.IModel;
import buddybox.core.State;

import static buddybox.core.events.AudioFocus.AUDIO_FOCUS_GAIN;
import static buddybox.core.events.AudioFocus.AUDIO_FOCUS_LOSS;
import static buddybox.core.events.AudioFocus.AUDIO_FOCUS_LOSS_TRANSIENT;
import static buddybox.core.events.Play.FINISHED_PLAYING;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.dispatch;
import static buddybox.ui.ModelProxy.removeStateListener;

public class Player extends Service {
    private final static int MAX_VOLUME = 100;
    private final static int DUCK_VOLUME = 30;

    private static Context context;
    private static MediaPlayer mediaPlayer;

    private static Handler handler = new Handler();
    private static boolean playCycleRunning = false;
    private static List<ProgressListener> listeners = new ArrayList<>();

    private static AudioManager audioManager;
    private static AudioManager.OnAudioFocusChangeListener audioFocusListener;

    private static boolean canPlay;

    private static State lastState;
    private IModel.StateListener stateListener;

    public static void init(Context context) {
        Player.context = context;

        Intent intent = new Intent(context, Player.class);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { @Override public void onCompletion(MediaPlayer mediaPlayer) {
            dispatch(FINISHED_PLAYING);
        }});

        stateListener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        addStateListener(stateListener);
    }

    @Override
    public void onDestroy() {
        removeStateListener(stateListener);
        super.onDestroy();
    }

    private void updateState(State state) {
        updatePlayerState(state);
        lastState = state;
    }

    private void updatePlayerState(State state) {
        // set player volume
        setNormalVolume(state);

        // seek to
        if (state.seekTo != null) {
            mediaPlayer.seekTo(state.seekTo);
            return;
        }

        // pause
        if (state.songPlaying == null || state.isPaused || state.songPlaying.isMissing) {
            if (lastState != null && lastState.songPlaying != null) {
                mediaPlayer.pause();
            }
            return;
        }

        // check if authorized to play
        if (!canPlay()) {
            dispatch(PLAY_PAUSE_CURRENT);
            return;
        }

        // play current song
        if (lastState.songPlaying == state.songPlaying) {
            mediaPlayer.start();
            startPlayCycle();
            return;
        }

        // play another song
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
    }

    private static boolean canPlay() {
        AudioManager audioManager = getAudioManager();
        if (audioManager == null)
            return false;

        // request audio focus
        if (!canPlay) {
            int result = audioManager.requestAudioFocus(audioFocusListener(), AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            canPlay = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            dispatch(AUDIO_FOCUS_GAIN);
        }
        return canPlay;
    }

    private static AudioManager.OnAudioFocusChangeListener audioFocusListener() {
        if (audioFocusListener == null) {
            audioFocusListener = new AudioManager.OnAudioFocusChangeListener() { @Override public void onAudioFocusChange(int i) {
                System.out.println(">>>> audio focus changed " + i);
                switch (i) {
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        setDuckVolume();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        canPlay = false;
                        dispatch(AUDIO_FOCUS_LOSS);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        canPlay = true;
                        dispatch(AUDIO_FOCUS_GAIN);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        dispatch(AUDIO_FOCUS_LOSS_TRANSIENT);
                        break;
                }
            }};
        }
        return audioFocusListener;
    }

    private static AudioManager getAudioManager() {
        if (audioManager == null)
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager;
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

    /**
     * Keep track of song activity_playing progress
     * and notify listeners
     */
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
        for (ProgressListener listener : listeners) {
            listener.updateProgress(mediaPlayer.getCurrentPosition());
        }
    }

    private static void setNormalVolume(State state) {
        int outputVolume = state.volumeSettings.get(state.outputActive);
        float playerVolume = outputVolume == 100
                ? 1f
                : (float) (1 - (Math.log(MAX_VOLUME - outputVolume) / Math.log(MAX_VOLUME)));
        mediaPlayer.setVolume(playerVolume, playerVolume);
    }

    private static void setDuckVolume() {
        int duck;
        if (lastState != null) {
            int currentVolume = lastState.volumeSettings.get(lastState.outputActive);
            duck = Math.min(currentVolume, DUCK_VOLUME);
        } else {
            duck = DUCK_VOLUME;
        }

        float duckVolume = (float) (1 - (Math.log(MAX_VOLUME - duck) / Math.log(MAX_VOLUME)));
        mediaPlayer.setVolume(duckVolume, duckVolume);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
