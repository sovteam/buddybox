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

public class AsyncImage2 {
    private static BlockingDeque<ImageView> viewQueue = new LinkedBlockingDeque<>();
    private static Map<ImageView, Playable> imagesToLoad = new ArrayMap<>();
    private static Map<ImageView, Bitmap> imagesLoaded = new ArrayMap<>();
    private static Object monitor = new Object();
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static int alternateResourceId;

        System.out.println("AsyncImage2.init()");
    public static void init(int alternateResourceId_) {
        alternateResourceId = alternateResourceId_;

        Thread loaderThread = new Thread() { @Override public void run() {
           while (true) {
               try {
                   System.out.println("AsyncImage2 waiting for a ImageView");
                   ImageView currentView = viewQueue.takeFirst();
                   System.out.println("AsyncImage2 got a currentView");
                   Playable myView = null;
                   synchronized (monitor) {
                       myView = imagesToLoad.get(currentView);
                   }
                   if (myView == null) continue;
                   Bitmap myImageToLoad = MediaInfoRetriever2.load(myView); // slow call
                   System.out.println("AsyncImage2 loaded an image");
                   synchronized (monitor) {
                       System.out.println("AsyncImage2 will check to use the new image");
                       if (myView == imagesToLoad.get(currentView)) { // may have changed
                           System.out.println("AsyncImage2 will try to use the new image");
                           imagesToLoad.remove(myView);
                           imagesLoaded.put(currentView, myImageToLoad);
                           if (imagesLoaded.size() == 1)
                               handler.post(new Runnable() { @Override public void run() {
                                   useLoadedImages();
                               }});
                       }
                   }
               } catch (InterruptedException e) {
                   e.printStackTrace();
               }
           }
        }};

        loaderThread.start();
    }

    public static void setImage(final ImageView view, final Playable playable, final int alternateResourceId) {
        AsyncImage2.alternateResourceId = alternateResourceId;
        synchronized (monitor) {
            System.out.println("AsyncImage2 setImage");
            imagesLoaded.remove(view);
            imagesToLoad.put(view, playable);
            viewQueue.addFirst(view);
        }
    }

    private static void useLoadedImages() {
        synchronized (monitor) {
            System.out.println("AsyncImage2 will use the new loaded image");
            for (Map.Entry<ImageView, Bitmap> entry : imagesLoaded.entrySet()) {
                if (entry.getValue() == null)
                    entry.getKey().setImageResource(alternateResourceId);
                else
                    entry.getKey().setImageBitmap(entry.getValue());
            }
            imagesLoaded.clear();
        }
    }
}
