package com.adalbertosoares.buddybox;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import buddybox.api.Core;
import buddybox.api.Play;
import buddybox.api.Playable;
import buddybox.api.Playlist;
import buddybox.api.Song;
import buddybox.api.VisibleState;

import static buddybox.CoreSingleton.dispatch;
import static buddybox.CoreSingleton.setStateListener;
import static buddybox.api.Play.PLAY_PAUSE_CURRENT;

public class MainActivity extends AppCompatActivity {

    private PlayablesArrayAdapter playables;
    private Playlist currentPlaylist;

    NotificationCompat.Builder notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set events
        ListView list = (ListView) findViewById(R.id.recentPlayables);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new Play(currentPlaylist, i));
        }});

        playables = new PlayablesArrayAdapter();
        list.setAdapter(playables);

        findViewById(R.id.playPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(PLAY_PAUSE_CURRENT);
        }});

        findViewById(R.id.playingMaximize).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            startActivity(new Intent(MainActivity.this, PlayingActivity.class));
        }});

        buildMainAppNotification();
    }

    private void buildMainAppNotification() {
        int notificationId = 123;
        notification = new NotificationCompat.Builder(this);
        notification.setAutoCancel(false);
        notification.setSmallIcon(R.drawable.ic_play);
        notification.setTicker("This is a ticker");
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("Notification Title");
        notification.setContentText("Notification Text");

        // Close app on dismiss
        Intent intentDismiss = new Intent(this, NotificationDismissedReceiver.class);
        intentDismiss.putExtra("com.my.app.notificationId", notificationId);
        PendingIntent pendingDelete = PendingIntent.getBroadcast(this, notificationId, intentDismiss, 0);
        notification.setDeleteIntent(pendingDelete);

        // TODO launch notification if closed when PLAY_PAUSE
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingContent = PendingIntent.getBroadcast(this, notificationId, intent, 0);
        notification.setContentIntent(pendingContent);

        // Play/Pause button
        Intent intentPlayPause = new Intent(this, NotificationPlayPauseReceiver.class);
        intentPlayPause.putExtra("com.my.app.notificationId", notificationId);
        PendingIntent pendingPlayPause = PendingIntent.getBroadcast(this, notificationId, intentPlayPause, 0);
        notification.addAction(R.drawable.ic_pause, "Play/Pause", pendingPlayPause);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(notificationId, notification.build());
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        void updateRecent(Playlist recent) {
            clear();
            addAll(recent.songs);
        }
    }

    private void updateState(VisibleState state) {
        // list songs
        playables.updateRecent(state.recent);
        currentPlaylist = state.recent;
        Song songPlaying = state.songPlaying;
        View playingBar = findViewById(R.id.playingBar);
        if (songPlaying == null)
            playingBar.setVisibility(View.INVISIBLE);
        else {
            playingBar.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.playingName)).setText(songPlaying.name());
            ((TextView)findViewById(R.id.playingSubtitle)).setText(songPlaying.subtitle());
            ((ImageButton)findViewById(R.id.playPause)).setImageResource(state.isPaused ? R.drawable.ic_play : R.drawable.ic_pause);
        }
    }
}
