package buddybox.io;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.media.session.MediaButtonReceiver;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.ui.ModelProxy;
import sov.buddybox.R;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.PAUSE;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.removeStateListener;

public class MediaPlayback extends Service {

    private static final int NOTIFICATION_ID = 999;
    private static MediaSessionCompat mediaSession;
    private IModel.StateListener stateListener;

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null)
                return;

            switch (action) {
                case "PLAY_PAUSE":
                    dispatch(PLAY_PAUSE_CURRENT);
                    break;
                case "SKIP_NEXT":
                    dispatch(SKIP_NEXT);
                    break;
                case "SKIP_PREVIOUS":
                    dispatch(SKIP_PREVIOUS);
                    break;
                default:
                    break;
            }
        }
    };

    public MediaPlayback() {}

    public static void init(Context context) {
        mediaSession = new MediaSessionCompat(context, "LOG TAG");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                Log.d("MediaPlayback", "onPlay");
                dispatch(PLAY_PAUSE_CURRENT);
                super.onPlay();
            }

            @Override
            public void onPause() {
                Log.d("MediaPlayback", "onPause");
                dispatch(PAUSE);
                super.onPause();
            }

            @Override
            public void onSkipToNext() {
                Log.d("MediaPlayback", "onSkipToNext");
                dispatch(SKIP_NEXT);
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                Log.d("MediaPlayback", "onSkipToPrevious");
                dispatch(SKIP_PREVIOUS);
                super.onSkipToPrevious();
            }

            @Override
            public void onStop() {
                Log.d("MediaPlayback", "onStop");
                super.onStop();
            }

            @Override
            public void onSeekTo(long pos) {
                Log.d("MediaPlayback", "onSkipTo");
                super.onSeekTo(pos);
            }
        });

        Intent intent = new Intent(context, MediaPlayback.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);

        IntentFilter filter = new IntentFilter();
        filter.addAction("PLAY_PAUSE");
        filter.addAction("SKIP_NEXT");
        filter.addAction("SKIP_PREVIOUS");
        filter.addAction("CloseApp");
        registerReceiver(receiver, filter);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (ModelProxy.isInitialized()) {
            stateListener = new IModel.StateListener() {
                @Override
                public void update(State state) {
                    updateState(state);
                }
            };
            addStateListener(stateListener);
        }
    }

    @Override
    public void onDestroy() {
        removeStateListener(stateListener);
        mediaSession.setActive(false);

        super.onDestroy();
    }

    private void updateState(State state) {
        setMediaPlaybackState(state);
        setMediaPlaybackMetadata(state);

        if (state.hasAudioFocus)
            mediaSession.setActive(true);
        else
            mediaSession.setActive(false);

        if (state.songPlaying == null)
            closeNotification();
        else
            updateNotification(state);
    }

    private void updateNotification(State state) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert nm != null;

        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSession);
        builder.setSmallIcon(R.drawable.ic_play);
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_skip_previous, getString(R.string.skip_previous),
                MediaStyleHelper.getActionIntent(this, "SKIP_PREVIOUS", KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)
        ));
        if (state.isPaused) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_play, getString(R.string.play),
                    MediaStyleHelper.getActionIntent(this, "PLAY_PAUSE", KeyEvent.KEYCODE_MEDIA_PLAY)
            ));
        } else {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_pause, getString(R.string.pause),
                    MediaStyleHelper.getActionIntent(this, "PLAY_PAUSE", KeyEvent.KEYCODE_MEDIA_PAUSE)
            ));
        }
        builder.addAction(new NotificationCompat.Action(
                R.drawable.ic_skip_next, getString(R.string.skip_next),
                MediaStyleHelper.getActionIntent(this, "SKIP_NEXT", KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD)
        ));
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession.getSessionToken())
        );
        nm.notify(NOTIFICATION_ID, builder.build());
    }

    private void closeNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null)
            return;

        nm.cancel(NOTIFICATION_ID);
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

    private static void setMediaPlaybackMetadata(State state) {
        Song song = state.songPlaying;
        if (song != null) {
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.name);
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist);
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album);
            builder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, song.genre);
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration);
//            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, song.getArt());
            mediaSession.setMetadata(builder.build());
            return;
        }
        mediaSession.setMetadata(null);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancelAll();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

