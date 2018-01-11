package buddybox.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.ArrayList;

import buddybox.api.Core;
import buddybox.api.Play;
import buddybox.api.Playable;
import buddybox.api.Playlist;
import buddybox.api.Song;
import buddybox.api.VisibleState;
import buddybox.impl.SongImpl;

import static buddybox.CoreSingleton.dispatch;
import static buddybox.CoreSingleton.setStateListener;
import static buddybox.api.Play.PLAY_PAUSE_CURRENT;

import static buddybox.api.Sampler.*;

public class MainActivity extends AppCompatActivity {

    private PlayablesArrayAdapter playables;
    private Playlist currentPlaylist;

    private LovedPlayablesArrayAdapter lovedPlayables;
    private Playlist lovedPlaylist;

    NotificationCompat.Builder mainNotification;
    private int notificationId = 0; // TODO move to a better place

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // library list
        ListView list = (ListView) findViewById(R.id.recentPlayables);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new Play(currentPlaylist, i));
        }});
        playables = new PlayablesArrayAdapter();
        list.setAdapter(playables);

        // Loved list
        ListView lovedList = (ListView) findViewById(R.id.lovedPlayables);
        lovedList.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new Play(lovedPlaylist, i));
        }});
        lovedPlayables = new LovedPlayablesArrayAdapter();
        lovedList.setAdapter(lovedPlayables);

        findViewById(R.id.whatshot).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            navigateTo(R.id.frameSampler);
        }});

        // Playing
        findViewById(R.id.playPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(PLAY_PAUSE_CURRENT);
        }});

        findViewById(R.id.playingMaximize).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            startActivity(new Intent(MainActivity.this, PlayingActivity.class));
        }});

        // NavBar
        findViewById(R.id.libraryNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            navigateTo(R.id.frameLibrary);
        }});

        findViewById(R.id.samplerNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            navigateTo(R.id.frameSampler);
        }});

        findViewById(R.id.lovedNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            navigateTo(R.id.frameLoved);
        }});

        findViewById(R.id.sharingNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            navigateTo(R.id.frameSharing);
        }});

        // Sampler
        findViewById(R.id.hateIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(SAMPLER_HATE);
        }});

        findViewById(R.id.loveIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(SAMPLER_LOVE);
        }});

        findViewById(R.id.deleteIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(SAMPLER_DELETE);
        }});

        navigateTo(R.id.frameLibrary);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(notificationId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setStateListener(new Core.StateListener() { @Override public void update(VisibleState state) {
            updateState(state);
        }});
    }

    private void updateState(VisibleState state) {
        updateLibraryState(state);
        updateSamplerState(state);
        updateLovedState(state);

        // Update new samplers count
        int samplerCount = state.samplerPlaylist == null ? 0 : state.samplerPlaylist.size();
        ((TextView)findViewById(R.id.newSamplerSongsCount)).setText(samplerCount == 0 ? "" : Integer.toString(samplerCount));

        // Update new loved count
        int lovedCount = state.lovedPlaylist == null ? 0 : state.lovedPlaylist.size();
        ((TextView)findViewById(R.id.newLovedSongsCount)).setText(lovedCount == 0 ? "" : Integer.toString(lovedCount));
    }

    private void updateLovedState(VisibleState state) {
        lovedPlayables.updateRecent(state.lovedPlaylist);
        lovedPlaylist = state.lovedPlaylist;

        if (state.lovedPlaylist == null || state.lovedPlaylist.isEmpty()) {
            findViewById(R.id.lovedEmpty).setVisibility(View.VISIBLE);
            findViewById(R.id.lovedPlayables).setVisibility(View.INVISIBLE);
        } else {
            findViewById(R.id.lovedEmpty).setVisibility(View.INVISIBLE);
            findViewById(R.id.lovedPlayables).setVisibility(View.VISIBLE);
        }
    }

    private void updateSamplerState(VisibleState state) {
        if (state.availableMemorySize < 10*1024) {
            findViewById(R.id.samplerLowMemory).setVisibility(View.VISIBLE);
            findViewById(R.id.samplerEmpty).setVisibility(View.INVISIBLE);
            findViewById(R.id.sampler).setVisibility(View.INVISIBLE);
            return;
        } else {
            findViewById(R.id.samplerLowMemory).setVisibility(View.INVISIBLE);
        }

        if (state.samplerPlaylist == null || state.samplerPlaylist.isEmpty()) {
            findViewById(R.id.samplerEmpty).setVisibility(View.VISIBLE);
            findViewById(R.id.sampler).setVisibility(View.INVISIBLE);
            return;
        } else {
            findViewById(R.id.samplerEmpty).setVisibility(View.INVISIBLE);
        }

        findViewById(R.id.sampler).setVisibility(View.VISIBLE);

        Song song = state.samplerPlaylist.song(0);
        ((TextView) findViewById(R.id.samplingName)).setText(song.name);
        ((TextView) findViewById(R.id.samplingArtist)).setText(song.artist);
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

            Playable item = getItem(position);
            setText(rowView, android.R.id.text1, item.name());
            setText(rowView, android.R.id.text2, item.subtitle());
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

    private class LovedPlayablesArrayAdapter extends PlayablesArrayAdapter {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = super.getView(position, convertView, parent);
            TextView text1 = (TextView) rowView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) rowView.findViewById(android.R.id.text2);
            Playable item = getItem(position);

            if (item.getClass() == SongImpl.class && ((SongImpl)item).isLoved()) {
                text1.setTextColor(Color.GREEN);
                text2.setTextColor(Color.GREEN);
            } else {
                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.WHITE);
            }
            return rowView;
        }
    }

    private void navigateTo(int frame) {
        if (frame == R.id.frameLibrary) {
            libraryActivate();
        } else {
            findViewById(R.id.frameLibrary).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.libraryText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.libraryNavbarBtn)).setImageResource(R.drawable.ic_library_music);
        }

        if (frame == R.id.frameSampler) {
            samplerActivate();
        } else {
            findViewById(R.id.frameSampler).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.samplerText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.samplerNavbarBtn)).setImageResource(R.drawable.ic_whatshot);
            dispatch(SAMPLER_STOP);
        }

        if (frame == R.id.frameLoved) {
            lovedActivate();
        } else {
            findViewById(R.id.frameLoved).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.lovedText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.lovedNavbarBtn)).setImageResource(R.drawable.ic_loved);
        }

        if (frame == R.id.frameSharing) {
            sharingActivate();
        } else {
            findViewById(R.id.frameSharing).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.sharingText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.sharingNavbarBtn)).setImageResource(R.drawable.ic_sharing);
        }
    }

    private void lovedActivate() {
        findViewById(R.id.frameLoved).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.lovedText)).setTextColor(Color.parseColor("#03a9f4"));
        ((ImageView) findViewById(R.id.lovedNavbarBtn)).setImageResource(R.drawable.ic_loved_blue);
    }

    private void sharingActivate() {
        findViewById(R.id.frameSharing).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.sharingText)).setTextColor(Color.parseColor("#03a9f4"));
        ((ImageView) findViewById(R.id.sharingNavbarBtn)).setImageResource(R.drawable.ic_sharing_blue);
    }

    private void samplerActivate() {
        findViewById(R.id.frameSampler).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.samplerText)).setTextColor(Color.parseColor("#03a9f4"));
        ((ImageView) findViewById(R.id.samplerNavbarBtn)).setImageResource(R.drawable.ic_whatshot_blue);

        // TODO remove main notification"

        dispatch(SAMPLER_START);
    }

    private void libraryActivate() {
        findViewById(R.id.frameLibrary).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.libraryText)).setTextColor(Color.parseColor("#03a9f4"));
        ((ImageView) findViewById(R.id.libraryNavbarBtn)).setImageResource(R.drawable.ic_library_music_blue);

        // TODO add main notification
    }

    private void updateLibraryState(VisibleState state) {
        Song songPlaying = state.songPlaying;
        View playingBar = findViewById(R.id.playingBar);
        playables.updateRecent(state.recent);
        currentPlaylist = state.recent;

        if (songPlaying == null) {
            playingBar.setVisibility(View.INVISIBLE);
        } else {
            playingBar.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.playingName)).setText(songPlaying.name());
            ((TextView)findViewById(R.id.playingSubtitle)).setText(songPlaying.subtitle());
            ((ImageButton)findViewById(R.id.playPause)).setImageResource(state.isPaused ? R.drawable.ic_play : R.drawable.ic_pause);
            updateMainNotification(state);
        }

        if (state.recent.songs.isEmpty()) {
            findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
            findViewById(R.id.recentPlayables).setVisibility(View.INVISIBLE);
            return;
        }
        findViewById(R.id.library_empty).setVisibility(View.INVISIBLE);
        findViewById(R.id.recentPlayables).setVisibility(View.VISIBLE);
    }

    private NotificationCompat.Builder mainNotification() {
        if (mainNotification == null) {
            mainNotification = new NotificationCompat.Builder(this);
            mainNotification.setAutoCancel(false);
            mainNotification.setSmallIcon(R.drawable.ic_play);

            // Close app on dismiss
            Intent intentDismiss = new Intent(this, NotificationDismissedReceiver.class);
            intentDismiss.putExtra("com.my.app.notificationId", notificationId);
            PendingIntent pendingDelete = PendingIntent.getBroadcast(this, notificationId, intentDismiss, 0);
            mainNotification.setDeleteIntent(pendingDelete);

            // Set focus to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingContent = PendingIntent.getBroadcast(this, notificationId, intent, 0);
            mainNotification.setContentIntent(pendingContent);
        }
        return mainNotification;
    }

    private void updateMainNotification(VisibleState state) {
        NotificationCompat.Builder notification = mainNotification();
        notification.mActions.clear();

        Song songPlaying = state.songPlaying;
        notification.setContentTitle(songPlaying.name);
        notification.setContentText(songPlaying.artist);
        notification.setTicker("Playing " + songPlaying.name);

        // Skip previous action
        Intent intentSkipPrevious = new Intent(this, NotificationSkipPreviousReceiver.class);
        intentSkipPrevious.putExtra("com.my.app.notificationId", notificationId);
        PendingIntent pendingPrevious = PendingIntent.getBroadcast(this, notificationId, intentSkipPrevious, 0);
        notification.addAction(R.drawable.ic_skip_previous, "", pendingPrevious);

        // Play/pause action
        Intent intentPlayPause = new Intent(this, NotificationPlayPauseReceiver.class);
        intentPlayPause.putExtra("com.my.app.notificationId", notificationId);
        PendingIntent pendingPlayPause = PendingIntent.getBroadcast(this, notificationId, intentPlayPause, 0);
        notification.addAction(state.isPaused ? R.drawable.ic_play : R.drawable.ic_pause, "", pendingPlayPause);

        // Skip next action
        Intent intentSkipNext = new Intent(this, NotificationSkipNextReceiver.class);
        intentSkipNext.putExtra("com.my.app.notificationId", notificationId);
        PendingIntent pendingNext = PendingIntent.getBroadcast(this, notificationId, intentSkipNext, 0);
        notification.addAction(R.drawable.ic_skip_next, "", pendingNext);

        notify(notificationId, notification);
    }

    private void notify(int id, NotificationCompat.Builder notificationBuilder) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(id, notificationBuilder.build());
    }
}