package buddybox.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.List;

import buddybox.core.Dispatcher;
import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.PlaylistSelected;
import buddybox.core.events.SeekTo;

import static buddybox.core.events.Play.SHUFFLE;
import static buddybox.ui.ModelProxy.dispatch;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.core.events.Play.REPEAT_SONG;
import static buddybox.core.events.Play.REPEAT_ALL;

public class PlayingActivity extends AppCompatActivity {

    private Song playing;
    private SeekBar seekBar;
    private boolean seekBarTouching;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);

        // Set events
        findViewById(R.id.playingMinimize).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            finish();
        }});
        findViewById(R.id.songMore).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { openSongOptionsDialog(); }});

        findViewById(R.id.playingPlayPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(PLAY_PAUSE_CURRENT); }});
        findViewById(R.id.skipNext).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(SKIP_NEXT);
        }});
        findViewById(R.id.skipPrevious).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {dispatch(SKIP_PREVIOUS);
        }});
        findViewById(R.id.repeatAll).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {dispatch(REPEAT_ALL);
        }});
        findViewById(R.id.repeatSong).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {dispatch(REPEAT_SONG);
        }});
        findViewById(R.id.shuffle).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(SHUFFLE);
        }});

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public int newPosition;

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
        ModelProxy.addStateListener(new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }});
    }

    private void updateState(State state) {
        if (state.songPlaying == null) {
            finish();
            return;
        }

        // Update seek bar
        if (!seekBarTouching)
            seekBar.setProgress(state.playProgress);

        if (playing != state.songPlaying)
            seekBar.setMax(state.songPlaying.duration);

        /*ObjectAnimator animation = ObjectAnimator.ofInt(seekBar, "progress", state.playProgress);
        animation.setInterpolator(new LinearInterpolator());
        animation.setDuration(1200);
        animation.start();*/

        seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.parseColor(state.isPaused ? "#4fc3f7" : "#43a047"), PorterDuff.Mode.MULTIPLY));
        seekBar.getThumb().setColorFilter(Color.parseColor(state.isPaused ? "#4fc3f7" : "#43a047"), PorterDuff.Mode.SRC_IN);
        playing = state.songPlaying;


        ((TextView) findViewById(R.id.playingSongName)).setText(playing.name);
        ((TextView) findViewById(R.id.playingSongArtist)).setText(playing.artist);
        ((TextView) findViewById(R.id.playingSongGenre)).setText(playing.genre);

        // Show playlists that includes song playing
        LinearLayout container = (LinearLayout)findViewById(R.id.playlistsChips);
        container.removeAllViews();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(12,0,12,0);
        List<Playlist> playlists = state.playlistsBySong.get(playing.hash.toString());
        if (playlists != null) {
            for (final Playlist p : playlists) {
                final TextView chip = new TextView(this);
                chip.setLayoutParams(params);
                chip.setText(p.name());
                chip.setBackgroundResource(state.playlistPlaying == p ? R.drawable.shape_chip_green : R.drawable.shape_chip_grey);
                chip.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                    dispatch(new PlaylistSelected(p));
                    startActivity(new Intent(chip.getContext(), PlaylistActivity.class));
                }});
                container.addView(chip);
            }
        }

        ((ImageView) findViewById(R.id.shuffle)).setImageResource(
                state.isShuffle ? R.drawable.ic_shuffle_blue : R.drawable.ic_shuffle);

        ((ImageView) findViewById(R.id.repeatSong)).setImageResource(
                state.repeatSong ? R.drawable.ic_repeat_one_blue : R.drawable.ic_repeat_one);

        ((ImageView) findViewById(R.id.repeatAll)).setImageResource(
                state.repeatAll ? R.drawable.ic_repeat_blue : R.drawable.ic_repeat);

        ((ImageButton)findViewById(R.id.playingPlayPause)).setImageResource(state.isPaused ? R.drawable.ic_play_circle : R.drawable.ic_pause_circle);
    }
}
