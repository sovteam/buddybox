package buddybox.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import buddybox.ModelSim;
import buddybox.core.IModel;
import buddybox.core.Playable;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;
import buddybox.core.events.SamplerDelete;
import buddybox.core.events.SamplerHate;
import buddybox.core.events.SamplerLove;
import buddybox.core.events.SetBluetoothVolume;
import buddybox.core.events.SetHeadphonesVolume;
import buddybox.core.events.SetSpeakerVolume;
import buddybox.io.BluetoothListener;
import buddybox.io.Library;
import buddybox.io.MediaInfoRetriever;
import buddybox.io.MediaPlayback;
import buddybox.io.Player;
import buddybox.io.Sampler;
import buddybox.model.Model;
import buddybox.ui.library.ArtistsFragment;
import buddybox.ui.library.PlaylistsFragment;
import buddybox.ui.library.RecentFragment;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Library.SYNC_LIBRARY;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Sampler.LOVED_VIEWED;
import static buddybox.core.events.Sampler.SAMPLER_START;
import static buddybox.core.events.Sampler.SAMPLER_STOP;
import static buddybox.core.events.SetHeadphonesVolume.HEADPHONES_CONNECTED;
import static buddybox.core.events.SetHeadphonesVolume.HEADPHONES_DISCONNECTED;
import static buddybox.model.Model.BLUETOOTH;
import static buddybox.model.Model.HEADPHONES;
import static buddybox.model.Model.SPEAKER;

public class MainActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback {

    private static boolean USE_SIMULATOR = false;
    private static final int WRITE_EXTERNAL_STORAGE = 1;

    private LovedPlayablesArrayAdapter lovedPlayables;
    private Playlist lovedPlaylist;

    private Song sampling;
    private HeadsetPlugReceiver headsetPlugReceiver;
    private SeekBar headphoneSeekBar;
    private SeekBar speakerSeekBar;
    private SeekBar bluetoothSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Library pager
        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Loved list
        ListView lovedList = findViewById(R.id.lovedPlayables);
        lovedList.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new Play(lovedPlaylist, i));
        }});
        lovedPlayables = new LovedPlayablesArrayAdapter();
        lovedList.setAdapter(lovedPlayables);

        findViewById(R.id.whatshot).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameSampler); }});

        // Playing
        findViewById(R.id.playPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(PLAY_PAUSE_CURRENT); }});
        findViewById(R.id.playingMaximize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, PlayingActivity.class));
                overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
            }
        });

        // NavBar
        findViewById(R.id.libraryNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameLibrary, view); }});
        findViewById(R.id.samplerNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameSampler, view); }});
        findViewById(R.id.lovedNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameLoved, view); }});
        findViewById(R.id.sharingNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameSharing, view); }});

        // Sampler
        findViewById(R.id.hateIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(new SamplerHate(sampling)); }});
        findViewById(R.id.loveIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(new SamplerLove(sampling)); }});
        findViewById(R.id.deleteIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(new SamplerDelete(sampling)); }});
        findViewById(R.id.grantPermission).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { checkWriteExternalStoragePermission(); }});

        // Sharing
        findViewById(R.id.syncLibrary).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {dispatch(SYNC_LIBRARY);
        }});

        navigateTo(R.id.frameLibrary);

        setHeadsetPlugObserver(); // TODO Extract to io class
        setVolumeControls();

        checkWriteExternalStoragePermission();
    }

    private void initApp() {
        ModelProxy.init(USE_SIMULATOR ? new ModelSim() : new Model(this));
        MediaInfoRetriever.init(this);
        Player.init(this);
        Library.init(this);
        Sampler.init(this);
        MediaPlayback.init(this);
        BluetoothListener.init(this);

        ModelProxy.addStateListener(new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }});

        dispatch(SYNC_LIBRARY);
    }


    private void checkWriteExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Request permission to write
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);

                View p = findViewById(R.id.permission);
                p.setVisibility(View.VISIBLE);
                p.bringToFront();

                return;
            }
        }
        findViewById(R.id.permission).setVisibility(View.GONE);
        initApp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                findViewById(R.id.permission).setVisibility(View.GONE);
                initApp();
            } else {
                // Permission denied
                Log.d("MainActivity", "WRITE_EXTERNAL_STORAGE: Permission denied");
            }
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new RecentFragment(), "RECENT");
        adapter.addFragment(new ArtistsFragment(), "ARTISTS");
        adapter.addFragment(new PlaylistsFragment(), "PLAYLISTS");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    protected void onDestroy() {
        if (headsetPlugReceiver != null) {
            unregisterReceiver(headsetPlugReceiver);
            headsetPlugReceiver = null;
        }
        super.onDestroy();
    }

    private void updateState(State state) {
        updateLibraryState(state);
        updateSamplerState(state);
        updateLovedState(state);
        updateSharing(state);

        // Update new samplers count
        int samplerCount = state.samplerPlaylist == null ? 0 : state.samplerPlaylist.size();
        ((TextView)findViewById(R.id.newSamplerSongsCount)).setText(samplerCount == 0 ? "" : Integer.toString(samplerCount));

        // Update new loved count
        int countNewLoved = 0;
        for (Song song : lovedPlaylist.songs) {
            if (!song.isLovedViewed())
                countNewLoved++;
        }
        ((TextView)findViewById(R.id.newLovedSongsCount)).setText(countNewLoved == 0 ? "" : Integer.toString(countNewLoved));
    }

    private void updateSharing(State state) {
        ((TextView)findViewById(R.id.songDuration)).setText(String.format(Locale.getDefault(), "%d", state.allSongsPlaylist.size()));

        if (state.syncLibraryPending) {
            findViewById(R.id.syncLibrarySpinner).setVisibility(View.VISIBLE);
            findViewById(R.id.syncLibrary).setEnabled(false);
            ((Button)findViewById(R.id.syncLibrary)).setText(R.string.Synchronizing);
        } else {
            findViewById(R.id.syncLibrarySpinner).setVisibility(View.GONE);
            findViewById(R.id.syncLibrary).setEnabled(true);
            ((Button)findViewById(R.id.syncLibrary)).setText(R.string.SyncLibrary);
        }

        ((TextView)findViewById(R.id.freeStorage)).setText(formatStorage(state.availableMemorySize));
        ((TextView)findViewById(R.id.mediaStorage)).setText(formatStorage(state.mediaStorageUsed));
        ProgressBar bar = findViewById(R.id.progressBar);
        bar.setMax((int) (state.availableMemorySize + state.mediaStorageUsed));
        bar.setProgress((int) state.mediaStorageUsed);

        // update output
        int ic_headphone = state.outputActive.equals(Model.HEADPHONES)
                ? state.isPaused
                    ? R.drawable.ic_headphones
                    : R.drawable.ic_headphones_blue
                : R.drawable.ic_headphones_grey;
        ((ImageView)findViewById(R.id.headphones)).setImageResource(ic_headphone);

        int ic_speaker = state.outputActive.equals(SPEAKER)
                ? state.isPaused
                    ? R.drawable.ic_speaker_phone
                    : R.drawable.ic_speaker_phone_blue
                : R.drawable.ic_speaker_phone_grey;
        ((ImageView)findViewById(R.id.speaker)).setImageResource(ic_speaker);

        int ic_bluetooth = state.outputActive.equals(Model.BLUETOOTH)
                ? state.isPaused
                    ? R.drawable.ic_bluetooth
                    : R.drawable.ic_bluetooth_blue
                : R.drawable.ic_bluetooth_grey;
        ((ImageView)findViewById(R.id.bluetooth)).setImageResource(ic_bluetooth);

        speakerSeekBar.setProgress(state.volumeSettings.get(SPEAKER));
        headphoneSeekBar.setProgress(state.volumeSettings.get(HEADPHONES));
        bluetoothSeekBar.setProgress(state.volumeSettings.get(BLUETOOTH));
    }

    private String formatStorage(Long storage) {
        double memory = (double) storage / 1024 / 1024;
        return (memory > 1024)
            ? (String.format(Locale.US, "%.1f", memory / 1024) + " GB")
            : (String.format(Locale.US, "%.1f", memory) + " MB");
    }

    private void updateLovedState(State state) {
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

    private void updateSamplerState(State state) {
        if (state.isSampling)
            sampling = state.songPlaying;
        else
            sampling = null;
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

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView == null
                    ? getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false)
                    : convertView;

            Playable item = getItem(position);
            if (item == null)
                return rowView;

            setText(rowView, android.R.id.text1, item.name());
            setText(rowView, android.R.id.text2, item.subtitle());
            return rowView;
        }

        private void setText(View rowView, int id, String value) {
            TextView textView = rowView.findViewById(id);
            textView.setText(value);
        }

        void updateRecent(Playlist recent) {
            clear();
            addAll(recent.songs);
        }
    }

    private class LovedPlayablesArrayAdapter extends PlayablesArrayAdapter {
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = super.getView(position, convertView, parent);
            TextView text1 = rowView.findViewById(android.R.id.text1);
            TextView text2 = rowView.findViewById(android.R.id.text2);
            Song item = (Song)getItem(position);
            if (item == null)
                return rowView;

            if (item.isLovedViewed()) {
                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.WHITE);
                text1.setTypeface(null, Typeface.NORMAL);
                text2.setTypeface(null, Typeface.NORMAL);
            } else {
                text1.setTextColor(Color.parseColor("#43a047"));
                text2.setTextColor(Color.parseColor("#43a047"));
                text1.setTypeface(null, Typeface.BOLD);
                text2.setTypeface(null, Typeface.NORMAL);
            }
            return rowView;
        }
    }

    private void navigateTo(int frame, View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        navigateTo(frame);
    }

    private void navigateTo(int frame) {
        boolean isSampling = findViewById(R.id.frameSampler).getVisibility() == View.VISIBLE;

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
            if (isSampling)
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
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dispatch(LOVED_VIEWED);
            }
        }, 2000);
        findViewById(R.id.frameLoved).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.lovedText)).setTextColor(Color.parseColor("#4fc3f7"));
        ((ImageView) findViewById(R.id.lovedNavbarBtn)).setImageResource(R.drawable.ic_loved_blue);
    }

    private void sharingActivate() {
        findViewById(R.id.frameSharing).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.sharingText)).setTextColor(Color.parseColor("#4fc3f7"));
        ((ImageView) findViewById(R.id.sharingNavbarBtn)).setImageResource(R.drawable.ic_sharing_blue);
    }

    private void samplerActivate() {
        findViewById(R.id.frameSampler).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.samplerText)).setTextColor(Color.parseColor("#4fc3f7"));
        ((ImageView) findViewById(R.id.samplerNavbarBtn)).setImageResource(R.drawable.ic_whatshot_blue);

        // TODO remove main notification"

        dispatch(SAMPLER_START);
    }

    private void libraryActivate() {
        findViewById(R.id.frameLibrary).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.libraryText)).setTextColor(Color.parseColor("#4fc3f7"));
        ((ImageView) findViewById(R.id.libraryNavbarBtn)).setImageResource(R.drawable.ic_library_music_blue);
    }

    private void updateLibraryState(State state) {
        Song songPlaying = state.songPlaying;
        View playingBar = findViewById(R.id.playingBar);

        if (songPlaying == null || state.isSampling) {
            playingBar.setVisibility(View.INVISIBLE);
        } else {
            playingBar.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.playingName)).setText(songPlaying.name());
            ((TextView)findViewById(R.id.playingSubtitle)).setText(songPlaying.subtitle());
            ((ImageButton)findViewById(R.id.playPause)).setImageResource(state.isPaused ? R.drawable.ic_play : R.drawable.ic_pause);
        }
    }

    private void setHeadsetPlugObserver() {
        headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(headsetPlugReceiver, intentFilter);
    }

    class HeadsetPlugReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Objects.equals(intent.getAction(), Intent.ACTION_HEADSET_PLUG)) {
                return;
            }
            boolean connectedHeadphones = (intent.getIntExtra("state", 0) == 1);
            // boolean connectedMicrophone = (intent.getIntExtra("microphone", 0) == 1) && connectedHeadphones;

            if (connectedHeadphones)
                dispatch(HEADPHONES_CONNECTED);
            else
                dispatch(HEADPHONES_DISCONNECTED);

            // String headsetName = intent.getStringExtra("name");
            // System.out.println(">>> connectedHeadphones " + connectedHeadphones + " " + headsetName);
        }
    }

    private void setVolumeControls() {
        headphoneSeekBar = findViewById(R.id.headphoneSeekBar);
        headphoneSeekBar.setMax(100);
        headphoneSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int newPosition;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                newPosition = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(newPosition);
                ModelProxy.dispatch(new SetHeadphonesVolume(newPosition));
            }
        });

        speakerSeekBar = findViewById(R.id.speakerSeekBar);
        speakerSeekBar.setMax(100);
        speakerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int newPosition;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                newPosition = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(newPosition);
                ModelProxy.dispatch(new SetSpeakerVolume(newPosition));
            }
        });

        bluetoothSeekBar = findViewById(R.id.bluetoothSeekBar);
        bluetoothSeekBar.setMax(100);
        bluetoothSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int newPosition;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                newPosition = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(newPosition);
                ModelProxy.dispatch(new SetBluetoothVolume(newPosition));
            }
        });
    }


}
