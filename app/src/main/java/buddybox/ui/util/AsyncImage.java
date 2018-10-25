package buddybox.ui.util;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import buddybox.core.Playable;
import buddybox.io.ImageUtils;

public class AsyncImage {

    static Map<ImageView, AsyncTask> tasksByView = new WeakHashMap<>();

    public static void setImage(final ImageView view, final Playable playable, final int alternateResourceId) {

        AsyncTask<Void, Void, Bitmap> task = new AsyncTask<Void, Void, Bitmap>() {

            private final WeakReference<ImageView> imageViewReference = new WeakReference<>(view);

            @Override
            protected Bitmap doInBackground(Void... v) {
                return ImageUtils.load(playable);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (isCancelled()) return;

                ImageView imageView = imageViewReference.get();
                if (imageView == null) return;

                if (bitmap == null)
                    imageView.setImageResource(alternateResourceId);
                else
                    imageView.setImageBitmap(bitmap);
            }
        };

        AsyncTask oldTask = tasksByView.put(view, task);
        if (oldTask != null)
            oldTask.cancel(true);

        task.execute();

    }
}
