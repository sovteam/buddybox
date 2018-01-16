package buddybox.sim;

import android.os.Handler;

import java.util.Arrays;

import buddybox.api.Model;
import buddybox.api.Playlist;
import buddybox.api.Song;
import buddybox.api.VisibleState;

public class ModelSim implements Model {

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
    public void dispatch(Model.Event event) { }

    @Override
    public void setStateListener(StateListener listener) {
        this.listener = listener;
        updateListener();

    }

    private void updateListener() {
        count++;
        Playlist recent = new Playlist(0, "Recent", Arrays.asList(
                new Song(1, "Mmmbop " + count, "Hanson", "Pop", null),
                new Song(2, "Xispas 1", "Cractus", "Chivas", null),
                new Song(3, "Xispas 2", "Cractus", "Chivas", null),
                new Song(4, "Xispas 3", "Cractus", "Chivas", null),
                new Song(5, "Mmmbop 4", "Hanson", "Pop", null),
                new Song(6, "Xispas 5", "Cractus", "Chivas", null),
                new Song(7, "Xispas 6", "Cractus", "Chivas", null),
                new Song(8, "Xispas 7", "Cractus", "Chivas", null),
                new Song(9, "Mmmbop 8", "Hanson", "Pop", null),
                new Song(10, "Xispas 9", "Cractus", "Chivas", null),
                new Song(11, "Xispas 10", "Cractus", "Chivas", null),
                new Song(12, "Xispas 11", "Cractus", "Chivas", null)));

        Song song = new Song(count, "Song " + count, "Artist " + count, "Genre " + count, null);

        boolean isPaused = count % 2 == 0;
        this.listener.update(new VisibleState(1, null, song, null, isPaused, null, null, null, null, null, 1, count * 1024, recent));
    }
}
