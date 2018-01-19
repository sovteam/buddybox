package buddybox.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import buddybox.api.Model;
import buddybox.api.Song;
import buddybox.api.State;

import static buddybox.ModelSingleton.dispatch;
import static buddybox.ModelSingleton.setStateListener;
import static buddybox.api.Play.PLAY_PAUSE_CURRENT;
import static buddybox.api.Play.SKIP_NEXT;
import static buddybox.api.Play.SKIP_PREVIOUS;

public class PlayingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playing);

        // Set events
        findViewById(R.id.playingPlayPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(PLAY_PAUSE_CURRENT);
        }});

        findViewById(R.id.playingMinimize).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            finish();
        }});

        findViewById(R.id.skipNext).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(SKIP_NEXT);
        }});

        findViewById(R.id.skipPrevious).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(SKIP_PREVIOUS);
        }});
    }

    @Override
    protected void onResume() {
        super.onResume();
        setStateListener(new Model.StateListener() { @Override public void update(State state) {
            updateState(state);
        }});
    }

    private void updateState(State state) {
        Song playing = state.songPlaying;
        if (playing != null) {
            ((TextView) findViewById(R.id.playingSongName)).setText(playing.name);
            ((TextView) findViewById(R.id.playingSongArtist)).setText(playing.artist);
            ((TextView) findViewById(R.id.playingSongGenre)).setText(playing.genre);
        }
        ((ImageButton)findViewById(R.id.playingPlayPause)).setImageResource(state.isPaused ? R.drawable.ic_play_circle : R.drawable.ic_pause_circle);
    }
}
