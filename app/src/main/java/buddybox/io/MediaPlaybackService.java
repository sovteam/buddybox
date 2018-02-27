package buddybox.io;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import com.adalbertosoares.buddybox.R;

import java.io.IOException;
import java.io.InputStream;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.ui.ModelProxy;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.PAUSE;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.ui.ModelProxy.addStateListener;
import static buddybox.ui.ModelProxy.removeStateListener;

public class MediaPlaybackService extends Service {

    private static final int NOTIFICATION_ID = 999;
    private static MediaSessionCompat mediaSession;
    private IModel.StateListener stateListener;

    private static final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println(">>> MediaPlaybackService Broadcast received!");
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

        Intent intent = new Intent(context, MediaPlaybackService.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println(">>> MediaPlaybackService onStartCommand");

        stateListener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        if (ModelProxy.isInitialized())
            addStateListener(stateListener);

        MediaButtonReceiver.handleIntent(mediaSession, intent);

        IntentFilter filter = new IntentFilter();
        filter.addAction("PLAY_PAUSE");
        filter.addAction("SKIP_NEXT");
        filter.addAction("SKIP_PREVIOUS");
        registerReceiver(receiver, filter);

        return super.onStartCommand(intent, flags, startId);
    }

    private void updateState(State state) {
        setMediaPlaybackState(state);
        setMediaPlaybackMetadata(this, state.songPlaying);

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
        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
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

    private static void setMediaPlaybackMetadata(Context context, Song song) {
        AssetManager assetManager = context.getAssets();
        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open("sneer2.jpg");
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (song != null) {
            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.name);
            builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist);
            builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album);
            builder.putString(MediaMetadataCompat.METADATA_KEY_GENRE, song.genre);
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration);
            if (bitmap != null)
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
            mediaSession.setMetadata(builder.build());
            return;
        }
        mediaSession.setMetadata(null);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            System.out.println(">>> Close Main Notification");
            manager.cancel(NOTIFICATION_ID);
        }
        super.onTaskRemoved(rootIntent);
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

