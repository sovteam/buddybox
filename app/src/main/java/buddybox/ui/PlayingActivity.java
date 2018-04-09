package buddybox.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.io.ByteArrayOutputStream;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.ArtistSelectedByName;
import buddybox.core.events.PlayPlaylist;
import buddybox.core.events.SeekTo;
import buddybox.io.Player;

import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.REPEAT;
import static buddybox.core.events.Play.SHUFFLE;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.core.events.Play.TOGGLE_DURATION_REMAINING;
import static buddybox.ui.ModelProxy.dispatch;

public class PlayingActivity extends AppCompatActivity {

    private Song playing;
    private Playlist playlist;
    private SeekBar seekBar;
    private boolean seekBarTouching;
    private IModel.StateListener stateListener;
    private Player.ProgressListener progressListener;
    private int lastProgress = 0;
    private boolean showDuration = true;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private int totalSongs;
    private boolean scrolling;
    private boolean isShuffle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        // Set events
        findViewById(R.id.minimize).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
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

        findViewById(R.id.playingSongArtist).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(new ArtistSelectedByName(playing.artist));
            startActivity(new Intent(getApplicationContext(), ArtistActivity.class));
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

        mPager = findViewById(R.id.songsPager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                // dispatch(new PlayPlaylist(playlist, position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (ViewPager.SCROLL_STATE_DRAGGING == state) {
                    scrolling = true;
                }
                if (ViewPager.SCROLL_STATE_IDLE == state) {
                    dispatch(new PlayPlaylist(playlist, mPager.getCurrentItem()));
                    scrolling = false;
                }
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

        stateListener = new IModel.StateListener() { @Override public void update(final State state) {
            Runnable runUpdate = new Runnable() {
                @Override
                public void run() {
                    updateState(state);
                }
            };
            handler.post(runUpdate);
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
        if (scrolling) {
            return;
        }

        updateSeekBar(state);

        playing = state.songPlaying;
        playlist = state.playlistPlaying;
        showDuration = state.showDuration;

        if (totalSongs != playlist.size() || isShuffle != state.isShuffle) {
            totalSongs = playlist.size();
            isShuffle = state.isShuffle;
            mPagerAdapter.notifyDataSetChanged();
        }

        int songIndex = state.playlistPlaying.songs.indexOf(state.songPlaying);
        if (state.isShuffle)
            songIndex = state.playlistPlaying.shuffledSongs().indexOf(songIndex);
        mPager.setCurrentItem(songIndex, false);

        updateSongDuration();
        updateTitle();
        updateSongInfo();
        updateCommandButtons(state);
    }

    private void updateSongInfo() {
        ((TextView) findViewById(R.id.playingSongName)).setText(playing.name);
        ((TextView) findViewById(R.id.playingSongArtist)).setText(playing.artist);
    }

    private void updateCommandButtons(State state) {
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

    private void updateSeekBar(State state) {
        if (playing != state.songPlaying) {
            seekBar.setProgress(0);
            seekBar.setMax(state.songPlaying.duration);
            ((TextView)findViewById(R.id.songProgress)).setText(state.songPlaying.formatTime(0));
        }
        seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.parseColor(state.isPaused ? "#FFFFFF" : "#03a9f4"), PorterDuff.Mode.MULTIPLY));
        seekBar.getThumb().setColorFilter(Color.parseColor(state.isPaused ? "#FFFFFF" : "#03a9f4"), PorterDuff.Mode.SRC_IN);
    }

    private void updateTitle() {
        String title = playlist.getClass() == Artist.class
                ? "Artist Playing"
                : playlist.getId() != 0
                ? "Playlist Playing"
                : "Song Playing";
        ((TextView) findViewById(R.id.playingTitle)).setText(title);
    }

    private void updateSongDuration() {
        String print = showDuration
                ? playing.formatTime(playing.duration)
                : "-" + playing.formatTime(playing.duration - lastProgress);
        ((TextView) findViewById(R.id.songDuration)).setText(print);
    }


    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment ret = new SongPageFragment();

            Bitmap bmp = playlist.song(position, isShuffle).getArt();
            Log.i("Playing", "New Fragment for: " + playlist.song(position, isShuffle).name + ", position: " + position);
            if (bmp != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                Bundle bundle = new Bundle();
                bundle.putByteArray("art", byteArray);
                ret.setArguments(bundle);
            }

            return ret;
        }

        @Override
        public int getCount() {
            return totalSongs;
        }
    }
}
