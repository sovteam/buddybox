package com.adalbertosoares.buddybox;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import buddybox.api.Core;
import buddybox.api.Playable;
import buddybox.api.VisibleState;

import static buddybox.CoreSingleton.dispatch;
import static buddybox.CoreSingleton.setStateListener;
import static buddybox.api.Song.PLAY_PAUSE_CURRENT;

public class MainActivity extends AppCompatActivity {

    private PlayablesArrayAdapter playables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set events
        ListView list = (ListView) findViewById(R.id.recentPlayables);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            System.out.println(">>> click i: " + i + " l: " + l + " view: " + view + " adapterView: " + adapterView);
        }});

        playables = new PlayablesArrayAdapter();
        list.setAdapter(playables);

        findViewById(R.id.playPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(PLAY_PAUSE_CURRENT);
        }});

        setStateListener(new Core.StateListener() { @Override public void update(VisibleState state) {
            updateState(state);
        }});



    }

    private class PlayablesArrayAdapter extends ArrayAdapter<Playable> {
        PlayablesArrayAdapter() {
            super(MainActivity.this, -1, new ArrayList<Playable>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView == null
                ? getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false)
                : convertView;

            setText(rowView, android.R.id.text1, getItem(position).name());
            setText(rowView, android.R.id.text2, getItem(position).subtitle());

            return rowView;
        }

        private void setText(View rowView, int id, String value) {
            TextView textView = (TextView) rowView.findViewById(id);
            textView.setText(value);
        }

        void updateRecent(List<Playable> recent) {
            clear();
            addAll(recent);
        }
    }

    private void updateState(VisibleState state) {
        // list songs
        playables.updateRecent(state.recent);

        ((TextView)findViewById(R.id.playingName)).setText(state.songPlaying.name());
        ((TextView)findViewById(R.id.playingSubtitle)).setText(state.songPlaying.subtitle());
        ((Button)findViewById(R.id.playPause)).setText(state.isPaused ? "PLAY" : "PAUSE");
    }
}



 /*
        File musicDirectory = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
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
            mPlayer.setDataSource(MainActivity.this, myUri);
            mPlayer.prepare();
            mPlayer.start();

            // mPlayer.pause();

        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println(musicDirectory);*/