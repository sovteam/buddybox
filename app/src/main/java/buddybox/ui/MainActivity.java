package buddybox.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import buddybox.ModelSim;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;
import buddybox.core.events.SetBluetoothVolume;
import buddybox.core.events.SetHeadphonesVolume;
import buddybox.core.events.SetSpeakerVolume;
import buddybox.core.events.SongFound;
import buddybox.core.events.SongSelected;
import buddybox.io.BluetoothListener;
import buddybox.io.HeadsetPlugListener;
import buddybox.io.Library;
import buddybox.io.MediaInfoRetriever2;
import buddybox.io.MediaPlayback;
import buddybox.io.Player;
import buddybox.io.Sampler;
import buddybox.io.SongUtils;
import buddybox.model.Model;
import buddybox.ui.library.ArtistsFragment;
import buddybox.ui.library.PlaylistsFragment;
import buddybox.ui.library.RecentFragment;
import buddybox.ui.util.AsyncImage;
import buddybox.web.ErrorLogger;
import sov.buddybox.BuildConfig;
import sov.buddybox.R;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Library.SYNC_LIBRARY;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.model.Model.BLUETOOTH;
import static buddybox.model.Model.HEADPHONES;
import static buddybox.model.Model.SPEAKER;

public class MainActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback {

    private static boolean USE_SIMULATOR = false;
    private static final int WRITE_EXTERNAL_STORAGE = 1;

    // TODO move to library loved page
    // private LovedPlayablesArrayAdapter lovedPlayables;
    // private Playlist lovedPlaylist;

    private Song sampling;
    private SeekBar headphoneSeekBar;
    private SeekBar speakerSeekBar;
    private SeekBar bluetoothSeekBar;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SearchFragment searchFrame;
    private int selectedFrame;
    private State lastState; // todo remove it when navigateTo switch to a new Event

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println(">>># onCreate!");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // catch all unhandled exceptions
        // TODO move to Application class
        if (!BuildConfig.DEBUG)
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    ErrorLogger.notify(ex);
                }
            });

        // Initialize AsyncImage loader
        AsyncImage.init(R.mipmap.sneer2);

        // Library pager
        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Loved list
        /* TODO move to library loved frame
        ListView lovedList = findViewById(R.id.lovedPlayables);
        lovedList.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new PlayPlaylist(lovedPlaylist, i));
        }});
        lovedPlayables = new LovedPlayablesArrayAdapter();
        lovedList.setAdapter(lovedPlayables);*/

        // TODO move to library loved frag
        // findViewById(R.id.whatshot).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameSampler); }});

        // Playing
        findViewById(R.id.playPause).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            if (lastState.songPlaying.isMissing) {
                Toast toast = Toast.makeText(getApplicationContext(), "Song is missing", Toast.LENGTH_SHORT);
                toast.show();
            } else {
                ModelProxy.dispatch(PLAY_PAUSE_CURRENT);
            }
        }});
        findViewById(R.id.playingMaximize).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, PlayingActivity.class));
                overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
            }
        });

        findViewById(R.id.grantPermission).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                checkWriteExternalStoragePermission();
            }
        });

        // NavBar
        findViewById(R.id.libraryNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameLibrary, view); }});
        // findViewById(R.id.samplerNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameSampler, view); }});
        findViewById(R.id.searchNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameSearch, view); }});
        findViewById(R.id.settingsNavBar).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { navigateTo(R.id.frameSettings, view); }});

        // Sampler
        /*findViewById(R.id.hateIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(new SamplerHate(sampling)); }});
        findViewById(R.id.loveIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(new SamplerLove(sampling)); }});
        findViewById(R.id.deleteIt).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(new SamplerDelete(sampling)); }});*/

        // Settings
        findViewById(R.id.syncLibrary).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {dispatch(SYNC_LIBRARY);
        }});

        navigateTo(R.id.frameLibrary);

        setVolumeControls();

        checkWriteExternalStoragePermission();

        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            System.out.println("Data >>># " + intent.getData());
            Uri uri = intent.getData();
            System.out.println("URI >>># " + uri);
            System.out.println("URI auth >>># " + uri.getAuthority());
            if (uri != null) {

                String path = getPath(this, uri);
                if (path == null)
                    return;
                System.out.println("Path >>> " + path);
                File mp3 = new File(path);
                Song song = SongUtils.readSong(mp3);
                dispatch(new SongFound(song));
                dispatch(new SongSelected(song.hash.toString()));
                dispatch(new Play(song));
                startActivity(new Intent(this, PlayingActivity.class));
            }
        }
    }


    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {


        // MediaStore (and general)
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    private void initApp() {
        ModelProxy.init(USE_SIMULATOR ? new ModelSim() : new Model(this));

        SongUtils.initializeContext(this.getApplication());
        MediaInfoRetriever2.init(this.getApplication());
        Player.init(this);
        Library.init();
        Sampler.init(this);
        MediaPlayback.init(this);
        BluetoothListener.init(this);
        HeadsetPlugListener.init(this);

        ModelProxy.addStateListener(new IModel.StateListener() { @Override public void update(final State state) {
            Runnable runUpdate = new Runnable() { @Override public void run() {
                    updateState(state);
            }};
            handler.post(runUpdate);
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
        if (!ModelProxy.isInitialized())
            initApp();
        else
            Log.i("MainActivity", "checkWriteExternalStoragePermission: APP already initiated");
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

    private void updateState(State state) {
        updateLibraryState(state);
        // updateSamplerState(state);
        // updateLovedState(state); TODO move to library fragment
        updateSettings(state);
        updateSearch(state);

        // Update new samplers count
        // int samplerCount = state.samplerPlaylist == null ? 0 : state.samplerPlaylist.size();
        // ((TextView)findViewById(R.id.newSamplerSongsCount)).setText(samplerCount == 0 ? "" : Integer.toString(samplerCount));

        // Update new loved count
        /* TODO move to library fragment
        int countNewLoved = 0;
        for (Song song : lovedPlaylist.songs) {
            if (!song.isLovedViewed())
                countNewLoved++;
        }
        ((TextView)findViewById(R.id.newLovedSongsCount)).setText(countNewLoved == 0 ? "" : Integer.toString(countNewLoved));
        */

        lastState = state;
    }

    private void updateSearch(State state) {
        if (selectedFrame == R.id.frameSearch && searchFrame != null) {
            searchFrame.updateState(state);
            System.out.println("MainActivity calls frame.updateSearch");
        }
    }

    private void updateSettings(State state) {
        ((TextView)findViewById(R.id.songsCount)).setText(String.format(Locale.getDefault(), "%d", state.allSongs.size()));

        if (state.syncLibraryPending) {
            findViewById(R.id.syncLibrarySpinner).setVisibility(View.VISIBLE);
            findViewById(R.id.syncLibrary).setEnabled(false);
            ((Button)findViewById(R.id.syncLibrary)).setText(R.string.Synchronizing);
        } else {
            findViewById(R.id.syncLibrarySpinner).setVisibility(View.GONE);
            findViewById(R.id.syncLibrary).setEnabled(true);
            ((Button)findViewById(R.id.syncLibrary)).setText(R.string.ScanDevice);
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

    /* TODO move to library loved frag
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
    }*/

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

        Song song = state.samplerPlaylist.song(0, state.isShuffle);
        ((TextView) findViewById(R.id.samplingName)).setText(song.name);
        ((TextView) findViewById(R.id.samplingArtist)).setText(song.artist);
    }

    /* TODO move to library loved page
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
    } */

    private void navigateTo(int frame, View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        navigateTo(frame);
    }

    private void navigateTo(int frame) {
        selectedFrame = frame;
        // boolean isSampling = findViewById(R.id.frameSampler).getVisibility() == View.VISIBLE;

        if (frame == R.id.frameLibrary) {
            libraryActivate();
        } else {
            findViewById(R.id.frameLibrary).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.libraryText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.libraryNavbarBtn)).setImageResource(R.drawable.ic_library_music);
        }

        /*if (frame == R.id.frameSampler) {
            samplerActivate();
        } else {
            findViewById(R.id.frameSampler).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.samplerText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.samplerNavbarBtn)).setImageResource(R.drawable.ic_whatshot);
            if (isSampling)
                dispatch(SAMPLER_STOP);
        }*/

        if (frame == R.id.frameSearch) {
            searchActivate();
        } else {
            if (searchFrame != null && searchFrame.getView() != null)
                searchFrame.getView().setVisibility(View.GONE);
            ((TextView) findViewById(R.id.searchText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.searchNavbarBtn)).setImageResource(R.drawable.ic_search);
        }

        if (frame == R.id.frameSettings) {
            sharingActivate();
        } else {
            findViewById(R.id.frameSettings).setVisibility(View.INVISIBLE);
            ((TextView) findViewById(R.id.settingsText)).setTextColor(Color.parseColor("#FFFFFF"));
            ((ImageView) findViewById(R.id.settingsNavbarBtn)).setImageResource(R.drawable.ic_settings);
        }
    }

    private void searchActivate() {
        if (searchFrame == null) {
            searchFrame = new SearchFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.framesContainer, searchFrame)
                    .commit();
        } else if (searchFrame.getView() != null) {
            searchFrame.getView().setVisibility(View.VISIBLE);
        }
        searchFrame.updateState(lastState);

        /* TODO move to library loved page
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dispatch(LOVED_VIEWED);
            }
        }, 2000);*/

        ((TextView) findViewById(R.id.searchText)).setTextColor(Color.parseColor("#4fc3f7"));
        ((ImageView) findViewById(R.id.searchNavbarBtn)).setImageResource(R.drawable.ic_search_blue);
    }

    private void sharingActivate() {
        findViewById(R.id.frameSettings).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.settingsText)).setTextColor(Color.parseColor("#4fc3f7"));
        ((ImageView) findViewById(R.id.settingsNavbarBtn)).setImageResource(R.drawable.ic_settings_blue);
    }

    /*private void samplerActivate() {
        findViewById(R.id.frameSampler).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.samplerText)).setTextColor(Color.parseColor("#4fc3f7"));
        ((ImageView) findViewById(R.id.samplerNavbarBtn)).setImageResource(R.drawable.ic_whatshot_blue);

        // TODO remove main notification"

        dispatch(SAMPLER_START);
    }*/

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

            int color = songPlaying.isMissing
                    ? Color.parseColor("#e53935")
                    : Color.WHITE;
            TextView name = findViewById(R.id.playingName);
            name.setText(songPlaying.name());
            name.setTextColor(color);
            TextView subtitle = findViewById(R.id.playingSubtitle);
            subtitle.setText(songPlaying.subtitle());
            subtitle.setTextColor(color);

            ((ImageButton)findViewById(R.id.playPause)).setImageResource(state.isPaused ? R.drawable.ic_play : R.drawable.ic_pause);
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
