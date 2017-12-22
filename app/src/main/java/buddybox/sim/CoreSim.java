package buddybox.sim;

import android.os.Handler;

import java.util.Arrays;
import java.util.List;

import buddybox.api.Core;
import buddybox.api.Playable;
import buddybox.api.Song;
import buddybox.api.VisibleState;

public class CoreSim implements Core {

    private StateListener listener;
    private int count = 0;
    private Handler handler = new Handler();

    {
        new Thread(){ {setDaemon(true);}
            @Override public void run() {
                while(true) {
                    sleepABit();
                    handler.post(new Runnable() { @Override public void run() {
                        updateListener();
                    }});
                }
            }
        }.start();
    }

    private void sleepABit() {
        try {
            Thread.sleep(3000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispatch(Core.Event event) { System.out.println(event); }

    @Override
    public void setStateListener(StateListener listener) {
        this.listener = listener;
        updateListener();

    }

    private void updateListener() {
        count++;
        List<Playable> recent = Arrays.asList(
                (Playable) new Song(1, "Mmmbop " + count, "Hanson", "Pop"),
                new Song(2, "Xispas 1", "Cractus", "Chivas"),
                new Song(3, "Xispas 2", "Cractus", "Chivas"),
                new Song(4, "Xispas 3", "Cractus", "Chivas"),
                new Song(5, "Mmmbop 4", "Hanson", "Pop"),
                new Song(6, "Xispas 5", "Cractus", "Chivas"),
                new Song(7, "Xispas 6", "Cractus", "Chivas"),
                new Song(8, "Xispas 7", "Cractus", "Chivas"),
                new Song(9, "Mmmbop 8", "Hanson", "Pop"),
                new Song(10, "Xispas 9", "Cractus", "Chivas"),
                new Song(11, "Xispas 10", "Cractus", "Chivas"),
                new Song(12, "Xispas 11", "Cractus", "Chivas"));

        Song song = new Song(count, "Song " + count, "Artist " + count, "Genre " + count);

        boolean isPaused = count % 2 == 0;
        this.listener.update(new VisibleState(1, 1, null, song, null, isPaused, null, null, null, recent, null, null));
    }
}
