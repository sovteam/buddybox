package buddybox.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.List;

import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.PlaylistSelected;
import buddybox.core.events.SeekTo;
import buddybox.io.Player;
import buddybox.ui.util.FlowLayout;

import static buddybox.core.events.Play.REPEAT;
import static buddybox.core.events.Play.SHUFFLE;
import static buddybox.core.events.Play.TOGGLE_DURATION_REMAINING;
import static buddybox.ui.ModelProxy.dispatch;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;

public class PlayingActivity extends AppCompatActivity {

    private Song playing;
    private SeekBar seekBar;
    private boolean seekBarTouching;
    private IModel.StateListener stateListener;
    private Player.ProgressListener progressListener;
    private int lastProgress = 0;
    private boolean showDuration = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println(">>> Playing onCreated");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        // Set events
        findViewById(R.id.playingMinimize).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            finish();
            overridePendingTransition(R.anim.stay,R.anim.slide_out_down);
        }});
        findViewById(R.id.songMore).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { openSongOptionsDialog(); }});
        findViewById(R.id.songDuration).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(TOGGLE_DURATION_REMAINING); }});

        findViewById(R.id.playingPlayPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(PLAY_PAUSE_CURRENT); }});
        findViewById(R.id.skipNext).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(SKIP_NEXT);
        }});
        findViewById(R.id.skipPrevious).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {dispatch(SKIP_PREVIOUS);
        }});
        findViewById(R.id.shuffle).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(SHUFFLE);
        }});
        findViewById(R.id.repeat).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {             dispatch(REPEAT);
        }});

        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int newPosition;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                newPosition = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarTouching = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(newPosition);
                dispatch(new SeekTo(newPosition));
                seekBarTouching = false;
            }
        });
    }

    private void openSongOptionsDialog() {
        if (playing == null)
            return;

        SongOptionsDialog dialog = new SongOptionsDialog();
        Bundle args = new Bundle();
        args.putString("songHash", playing.hash.toString());
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "Song Options");
    }

    @Override
    protected void onResume() {
        super.onResume();
        stateListener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        ModelProxy.addStateListener(stateListener);
        progressListener = new Player.ProgressListener() { @Override public void updateProgress(int progress) { updatePlayerProgress(progress);
        }};
        Player.addListener(progressListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ModelProxy.removeStateListener(stateListener);
        Player.removeListener(progressListener);
    }

    private void updatePlayerProgress(int progress) {
        if (!seekBarTouching) {
            seekBar.setProgress(progress);
        }
        if (playing != null) {
            ((TextView) findViewById(R.id.songProgress)).setText(playing.formatTime(progress));
            if (!showDuration)
                ((TextView) findViewById(R.id.songDuration)).setText(String.format("-%s", playing.formatTime(playing.duration - progress)));
        }
        lastProgress = progress;
    }

    private void updateState(State state) {
        if (state.songPlaying == null) {
            finish();
            return;
        }

        // Update seek bar
        if (playing != state.songPlaying) {
            seekBar.setProgress(0);
            seekBar.setMax(state.songPlaying.duration);
            ((TextView)findViewById(R.id.songProgress)).setText(state.songPlaying.formatTime(0));
        }
        seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.parseColor(state.isPaused ? "#FFFFFF" : "#03a9f4"), PorterDuff.Mode.MULTIPLY));
        seekBar.getThumb().setColorFilter(Color.parseColor(state.isPaused ? "#FFFFFF" : "#03a9f4"), PorterDuff.Mode.SRC_IN);

        playing = state.songPlaying;

        showDuration = state.showDuration;
        updateSongDuration();

        ((ImageView) findViewById(R.id.art)).setImageBitmap(playing.getArt());

        ((TextView) findViewById(R.id.playingSongName)).setText(playing.name);
        ((TextView) findViewById(R.id.playingSongArtist)).setText(playing.artist);
        ((TextView) findViewById(R.id.playingSongGenre)).setText(playing.genre);

        // Show playlists that includes song activity_playing
        FlowLayout container = findViewById(R.id.playlistsChips);
        container.removeAllViews();

        List<Playlist> playlists = state.playlistsBySong.get(playing.hash.toString());
        if (playlists != null && !playlists.isEmpty()) {
            TextView label = new TextView(this);
            label.setText("In playlists: ");
            label.setPadding(0,30,10,12);
            container.addView(label);

            for (final Playlist p : playlists) {
                final TextView chip = new TextView(this);
                chip.setText(p.name());
                chip.setTextColor(Color.parseColor(state.playlistPlaying == p ? "#03a9f4" : "#FFFFFF" ));
                chip.setBackgroundResource(R.drawable.shape_chip_grey);
                chip.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                    dispatch(new PlaylistSelected(p));
                    startActivity(new Intent(chip.getContext(), PlaylistActivity.class));
                }});
                container.addView(chip);
            }
        }

        ((ImageView) findViewById(R.id.shuffle)).setImageResource(
                state.isShuffle ? R.drawable.ic_shuffle_blue : R.drawable.ic_shuffle);

        ((ImageView) findViewById(R.id.repeat)).setImageResource(
            state.repeatSong
                ? R.drawable.ic_repeat_one_blue
                : state.repeatAll
                    ? R.drawable.ic_repeat_blue
                    : R.drawable.ic_repeat);

        ((ImageButton)findViewById(R.id.playingPlayPause)).setImageResource(state.isPaused ? R.drawable.ic_play_circle : R.drawable.ic_pause_circle);
    }

    private void updateSongDuration() {
        String print = showDuration
                ? playing.formatTime(playing.duration)
                : "-" + playing.formatTime(playing.duration - lastProgress);
        ((TextView) findViewById(R.id.songDuration)).setText(print);
    }
}
