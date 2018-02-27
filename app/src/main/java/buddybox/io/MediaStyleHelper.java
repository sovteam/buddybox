package buddybox.io;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

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
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                // .setDeleteIntent(PendingIntent.getActionIntent(context, KeyEvent.KEYCODE_MEDIA_PAUSE));
        return builder;
    }

    static PendingIntent getActionIntent(Context context, String action, int keycode) {
        Intent intent = new Intent("Action " + keycode);
        intent.setAction(action);
        intent.putExtra("keycode", keycode);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
