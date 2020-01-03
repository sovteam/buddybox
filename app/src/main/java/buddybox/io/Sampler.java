package buddybox.io;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.List;

import buddybox.core.Dispatcher;
import buddybox.core.Song;
import buddybox.core.events.SamplerDelete;
import buddybox.core.events.SamplerHate;
import buddybox.core.events.SamplerLove;
import buddybox.core.events.SamplerUpdated;
import buddybox.core.events.SongAdded;

import static buddybox.ui.ModelProxy.dispatch;

public class Sampler {
    private static Context context;

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

        File newFile = new File(SongUtils.musicFolder(), event.song.filePath); // TODO check filePath of sampler song
        File currentFile = new File(event.song.filePath);
        boolean moved = currentFile.renameTo(newFile); // TODO copy file
        if (moved) {
            dispatch(new SongAdded(event.song)); // TODO remove
            event.song.setLoved();
        } else {
            System.out.println("File could not be moved to music folder");
        }

    }

    private static void deleteSong(Song song) {
        File file = new File(samplerDirectory(), song.filePath);
        if (file.delete())
            System.out.println("Unable to delete file: " + file.getPath());
    }

    private static void updateSamplerLibrary() {
        List<Song> samples = SongUtils.listSongs(samplerDirectory());
        dispatch(new SamplerUpdated(samples));
    }

    private static File samplerDirectory() {
        // TODO: remove mkdirs, new API29 method getExternalFilesDir creates missing folders
        File ret = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (ret != null)
            if (!ret.exists() && !ret.mkdirs())
                System.out.println("Unable to create folder: " + ret);
        return ret;
    }


}
