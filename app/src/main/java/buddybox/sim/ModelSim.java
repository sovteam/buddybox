package buddybox.sim;

import android.os.Handler;

import java.util.Arrays;

import buddybox.core.Hash;
import buddybox.core.Model;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;

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
    public void addStateListener(StateListener listener) {
        this.listener = listener;
        updateListener();

    }

    private void updateListener() {
        count++;
        Playlist recent = new Playlist(0, "Recent", Arrays.asList(
                new Song(new Hash(new byte[]{1}), "Mmmbop " + count, "Hanson", "Pop", 10, null, null, null),
                new Song(new Hash(new byte[]{2}), "Xispas 1", "Cractus", "Chivas", 11, null, null, null),
                new Song(new Hash(new byte[]{3}), "Xispas 2", "Cractus", "Chivas", 12, null, null, null),
                new Song(new Hash(new byte[]{4}), "Xispas 3", "Cractus", "Chivas", 13, null, null, null),
                new Song(new Hash(new byte[]{5}), "Mmmbop 4", "Hanson", "Pop", 14, null, null, null),
                new Song(new Hash(new byte[]{6}), "Xispas 5", "Cractus", "Chivas", 15, null, null, null),
                new Song(new Hash(new byte[]{7}), "Xispas 6", "Cractus", "Chivas", 16, null, null, null),
                new Song(new Hash(new byte[]{8}), "Xispas 7", "Cractus", "Chivas", 17, null, null, null),
                new Song(new Hash(new byte[]{9}), "Mmmbop 8", "Hanson", "Pop", 18, null, null, null),
                new Song(new Hash(new byte[]{10}), "Xispas 9", "Cractus", "Chivas", 19, null, null, null),
                new Song(new Hash(new byte[]{11}), "Xispas 10", "Cractus", "Chivas", 20, null, null, null),
                new Song(new Hash(new byte[]{12}), "Xispas 11", "Cractus", "Chivas", 21, null, null, null)));

        Song song = new Song(new Hash(new byte[]{13}), "Song " + count, "Artist " + count, "Genre " + count, 11, null, null, null);

        boolean isPaused = count % 2 == 0;
        this.listener.update(new State(1, null, song, null, isPaused, null, false, null, null, null, null, 1, count * 1024, recent, null, true));
    }
}
