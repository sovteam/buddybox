package buddybox.controller;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import buddybox.core.Dispatcher;
import buddybox.core.events.LovedUpdated;
import buddybox.core.events.SamplerDelete;
import buddybox.core.events.SamplerHate;
import buddybox.core.events.SamplerLove;
import buddybox.core.events.SamplerUpdated;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.SongAdded;

import static buddybox.ModelSingleton.dispatch;

public class Sampler {
    private static Context context;
    private static List<Song> samples;
    private static List<Song> loved = new ArrayList<>();

    public static void init(Context context) {
        Sampler.context = context;
        Dispatcher.addListener(new Dispatcher.Listener() { @Override public void onEvent(Dispatcher.Event event) {
            handle(event);
        }});

        updateSamplerLibrary();
    }

    private static void handle(Dispatcher.Event event) {
        if (event.getClass() == SamplerDelete.class)
            samplerDelete((SamplerDelete) event);

        if (event.getClass() == SamplerLove.class)
            samplerLove((SamplerLove) event);

        if (event.getClass() == SamplerHate.class)
            samplerHate((SamplerHate) event);
    }

    private static void samplerDelete(SamplerDelete event) {
        System.out.println(">>> Sampler Delete Song " + event.song.name);
        // todo event.song.setDeleted();
        deleteSong(event.song);
    }

    private static void samplerHate(SamplerHate event) {
        System.out.println(">>> Sampler Hate Song " + event.song.name);
        // todo event.song.setHated();
        deleteSong(event.song);
    }

    private static void samplerLove(SamplerLove event) {
        System.out.println(">>> Sampler Love Song " + event.song.name);
        event.song.setLoved();
        SongUtils.moveToLibrary(event.song);
        loved.add(event.song);
        dispatch(new LovedUpdated(loved));
        dispatch(new SongAdded(event.song));
    }

    private static void deleteSong(Song song) {
        if (song.file.delete())
            System.out.println("Unable to delete file: " + song.file.getPath());
    }

    private static void updateSamplerLibrary() {
        samples = SongUtils.listSongs(samplerDirectory());
        dispatch(new SamplerUpdated(samples));
    }

    private static File samplerDirectory() {
        File ret = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (ret != null)
            if (!ret.exists() && !ret.mkdirs())
                System.out.println("Unable to create folder: " + ret);
        return ret;
    }


}
