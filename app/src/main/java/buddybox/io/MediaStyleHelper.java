package buddybox.io;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;

import buddybox.ui.MainActivity;
import buddybox.ui.PlayingActivity;

public class MediaStyleHelper {

    public static NotificationCompat.Builder from (Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "MainNotification");
        builder
                .setShowWhen(false)
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(description.getIconBitmap())
                .setContentIntent(controller.getSessionActivity())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDeleteIntent(getActionIntent(context, "CloseApp", KeyEvent.KEYCODE_MEDIA_PAUSE));

        Intent resultIntent = new Intent(context, PlayingActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(context,
                        42,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        return builder;
    }

    static PendingIntent getActionIntent(Context context, String action, int keycode) {
        Intent intent = new Intent("Action " + keycode);
        intent.setAction(action);
        intent.putExtra("keycode", keycode);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
