package buddybox.ui.util;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;
import android.widget.ImageView;

import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import buddybox.core.Playable;
import buddybox.io.MediaInfoRetriever2;

public class AsyncImage {

    private static BlockingDeque<ImageView> viewStack = new LinkedBlockingDeque<>();
    private static Map<ImageView, Playable> imagesToLoad = new ArrayMap<>();
    private static Map<ImageView, Bitmap> imagesLoaded = new ArrayMap<>();

    private static Handler handler = new Handler(Looper.getMainLooper());
    private static int alternateResourceId;

    public static void init(int alternateResourceId_) {
        alternateResourceId = alternateResourceId_;

        new Thread() { @Override public void run() {
           while (true) {
               ImageView myView = pop(viewStack);
               Playable myImageToLoad = imageToLoad(myView);
               if (myImageToLoad == null)
                   continue;

               Bitmap myLoadedImage = MediaInfoRetriever2.load(myImageToLoad); // slow call

               imageLoaded(myView, myImageToLoad, myLoadedImage);
           }
        }}.start();
    }

    synchronized
    public static void setImage(final ImageView view, final Playable playable) {
        imagesLoaded.remove(view);
        imagesToLoad.put(view, playable);
        viewStack.addFirst(view);
    }

    synchronized
    private static void imageLoaded(ImageView myView, Playable myImageToLoad, Bitmap myLoadedImage) {
        if (myImageToLoad != imagesToLoad.get(myView)) // may have changed
            return;
        imagesToLoad.remove(myView);
        imagesLoaded.put(myView, myLoadedImage);
        if (imagesLoaded.size() == 1)
            handler.post(new Runnable() { @Override public void run() {
                useLoadedImages();
            }});
    }

    synchronized // Synchronized. Don't inline.
    private static Playable imageToLoad(ImageView myView) {
        return imagesToLoad.get(myView);
    }

    synchronized
    private static void useLoadedImages() {
        for (Map.Entry<ImageView, Bitmap> entry : imagesLoaded.entrySet())
            if (entry.getValue() == null)
                entry.getKey().setImageResource(alternateResourceId);
            else
                entry.getKey().setImageBitmap(entry.getValue());
        imagesLoaded.clear();
    }

    static private <T> T pop(BlockingDeque<T> stack) {
        try {
            return stack.takeFirst();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
