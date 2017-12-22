package buddybox.impl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buddybox.api.Core;
import buddybox.api.Playable;
import buddybox.api.Song;
import buddybox.api.VisibleState;

public class CoreImpl implements Core {

    private final Context context;
    private final MediaPlayer player;
    private final Handler handler = new Handler();

    private int nextId = 0;
    private final Map<Integer, File> songFilesById = new HashMap<>();

    private StateListener listener;
    private File musicDirectory;
    private Song currentSong;

    public CoreImpl(Context context) {
        this.context = context;

        musicDirectory = this.context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        System.out.println(musicDirectory);

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }


    @Override
    public void dispatch(Event event) {
        if (event == Song.PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event.getClass() == Playable.Play.class) play((Playable.Play)event);
    }

    private void play(Playable.Play event) {
        int id = event.playableId;
        File file = songFilesById.get(id);
        currentSong = readSongMetadata(id, file);
        try {
            Uri myUri = Uri.parse(file.getCanonicalPath());
            player.reset();
            player.setDataSource(context, myUri);
            player.prepare();
            player.start();
            updateListener();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void playPauseCurrent() {
        if (player.isPlaying())
            player.pause();
        else
            player.start();

        updateListener();
    }


    private void updateListener() {
        Runnable runnable = new Runnable() { @Override public void run() {
            VisibleState state = new VisibleState(1, 1, null, currentSong, null, !player.isPlaying(), null, null, null, listMP3Songs(), null, null);
            listener.update(state);
        }};
        handler.post(runnable);
    }

    private List<Playable> listMP3Songs() {
        List<Playable> ret = new ArrayList<>();
        File[] files = musicDirectory.listFiles();
        songFilesById.clear();
        for (File file : files) {
            if (!file.getName().toLowerCase().endsWith(".mp3"))
                continue;
            int id = nextId++;
            songFilesById.put(id, file);
            ret.add(readSongMetadata(id, file));
        }
        return ret;
    }

    @NonNull
    private Song readSongMetadata(int id, File file) {
        return new Song(id,
                file.getName().substring(0, file.getName().length() - 4),
                "Unknown Artist",
                "Unknown Genre");
    }

    @Override
    public void setStateListener(StateListener listener) {
        this.listener = listener;
        updateListener();
    }
}
