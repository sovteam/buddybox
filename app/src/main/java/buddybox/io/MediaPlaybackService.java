package buddybox.io;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import buddybox.core.IModel;
import buddybox.core.State;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.PAUSE;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.removeStateListener;

public class MediaPlaybackService extends Service {

    private static MediaSessionCompat mediaSession;
    private IModel.StateListener stateListener;

    public MediaPlaybackService() {}

    public static void init(Context context) {
        System.out.println(">>> INIT MediaPlaybackService");
        mediaSession = new MediaSessionCompat(context, "LOG TAG");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                System.out.println(">>> MEDIA onPlay");
                dispatch(PLAY_PAUSE_CURRENT);
                super.onPlay();
            }

            @Override
            public void onPause() {
                System.out.println(">>> MEDIA onPause");
                dispatch(PAUSE);
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                System.out.println(">>> MEDIA onSkipToNext");
                dispatch(SKIP_NEXT);
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                System.out.println(">>> MEDIA onSkipToPrevious");
                dispatch(SKIP_PREVIOUS);
                super.onSkipToPrevious();
            }

            @Override
            public void onStop() {
                System.out.println(">>> MEDIA onStop");
                super.onStop();
            }

            @Override
            public void onSeekTo(long pos) {
                System.out.println(">>> MEDIA onSkipTo");
                super.onSeekTo(pos);
            }
        });

        /*
        updateState(State state) {
            mediaSession.setActive(state.hasAudioFocus);
        }
         */
        mediaSession.setActive(true);

        Intent intent = new Intent(context, MediaPlaybackService.class);
        context.startService(intent);
    }

    private static void setMediaPlaybackState(State state) {
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
        long actions = PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;

        if (state.isPaused) {
            actions = actions | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY;
        } else {
            actions = actions | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE;
        }
        builder.setActions(actions);

        int playerState = state.isStopped
                ? PlaybackStateCompat.STATE_STOPPED
                : state.isPaused
                    ? PlaybackStateCompat.STATE_PAUSED
                    : PlaybackStateCompat.STATE_PLAYING;

        builder.setState(playerState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mediaSession.setPlaybackState(builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println(">>> MediaPlaybackService onStartCommand");
        stateListener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        addStateListener(stateListener);
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateState(State state) {
        setMediaPlaybackState(state);
    }

    @Override
    public void onDestroy() {
        removeStateListener(stateListener);
        mediaSession.setActive(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
