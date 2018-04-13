package buddybox;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import buddybox.core.IModel;
import buddybox.core.Playable;
import buddybox.core.Song;
import buddybox.core.State;
import sov.Hash;
import utils.Daemon;

import static utils.Utils.sleepQuietly;

public class ModelSim implements IModel {

    private List<StateListener> listeners = new ArrayList<>();
    private int count = 0;
    private Handler handler = new Handler();

    {
        new Daemon("Simulator") { @Override public void run() {
            while (true) {
                sleepQuietly(3000);
                handler.post(new Runnable() { @Override public void run() {
                        updateListener();
                    }});
            }
        }};
    }

    @Override
    public void addStateListener(StateListener listener) {
        listeners.add(listener);
        updateListener();

    }

    @Override
    public void removeStateListener(StateListener listener) {
        listeners.remove(listener);
    }

    private void updateListener() {
        count++;
        List<Song> songs = new ArrayList<>(Arrays.asList(
                new Song(1L, new Hash(new byte[]{1}), "Mmmbop " + count, "Hanson", "", "Pop", 10, null, 1, 1, false, false, 1L),
                new Song(2L, new Hash(new byte[]{2}), "Xispas 1", "Cractus", "", "Chivas", 11, null, 1, 1, false, false, 1L),
                new Song(3L, new Hash(new byte[]{3}), "Xispas 2", "Cractus", "", "Chivas", 12, null, 1, 1, false, false, 1L),
                new Song(4L, new Hash(new byte[]{4}), "Xispas 3", "Cractus", "", "Chivas", 13, null, 1, 1, false, false, 1L),
                new Song(5L, new Hash(new byte[]{5}), "Mmmbop 4", "Hanson", "", "Pop", 14, null, 1, 1, false, false, 1L),
                new Song(6L, new Hash(new byte[]{6}), "Xispas 5", "Cractus", "", "Chivas", 15, null, 1, 1, false, false, 1L),
                new Song(7L, new Hash(new byte[]{7}), "Xispas 6", "Cractus", "", "Chivas", 16, null, 1, 1, false, false, 1L),
                new Song(8L, new Hash(new byte[]{8}), "Xispas 7", "Cractus", "", "Chivas", 17, null, 1, 1, false, false, 1L),
                new Song(9L, new Hash(new byte[]{9}), "Mmmbop 8", "Hanson", "", "Pop", 18, null, 1, 1, false, false, 1L),
                new Song(10L, new Hash(new byte[]{10}), "Xispas 9", "Cractus", "", "Chivas", 19, null, 1, 1, false, false, 1L),
                new Song(11L, new Hash(new byte[]{11}), "Xispas 10", "Cractus", "", "Chivas", 20, null, 1, 1, false, false, 1L),
                new Song(12L, new Hash(new byte[]{12}), "Xispas 11", "Cractus", "", "Chivas", 21, null, 1, 1, false, false, 1L)));

        Song song = new Song(13L, new Hash(new byte[]{13}), "Song " + count, "Artist " + count, "", "Genre " + count, 11, null, 1, 1, false, false, 1L);

        boolean isPaused = count % 2 == 0;
        for (StateListener listener : listeners)
            listener.update(new State(1, null, new ArrayList<Playable>(), song, null, null, false, isPaused, false, false, false, true, null, false, null, null, null, null, 1, count * 1024, 0L, songs, null, false, null, null, null, null, new HashMap<String, Integer>(), true, null, null));
    }
}
