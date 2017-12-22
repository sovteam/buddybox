package buddybox.impl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

import buddybox.api.Core;

public class CoreImpl implements Core {

    private final Context context;

    public CoreImpl(Context context) {

        this.context = context;

        File musicDirectory = this.context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        System.out.println(">>> Environment: " + Environment.DIRECTORY_MUSIC);
        for (File file : musicDirectory.listFiles()) {
            System.out.println(">>> File: " + file.getName());
        }




        File mp3 = musicDirectory.listFiles()[0];

        try {
            MediaPlayer mPlayer = new MediaPlayer();
            Uri myUri = Uri.parse(mp3.getCanonicalPath());
            System.out.println(">>> URI: " + myUri);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(this.context, myUri);
            mPlayer.prepare();
            mPlayer.start();

            // mPlayer.pause();

        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println(musicDirectory);
    }


    @Override
    public void dispatch(Event event) {

    }

    @Override
    public void setStateListener(StateListener listener) {

    }
}
