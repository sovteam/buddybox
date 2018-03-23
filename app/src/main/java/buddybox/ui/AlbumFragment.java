package buddybox.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.List;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;

public class AlbumFragment extends Fragment {

    private static final String ALBUM = "album";
    private String album;

    private View view;
    private IModel.StateListener listener;
    private LayoutInflater inflater;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public AlbumFragment() { }

    public static AlbumFragment newInstance(String album) {
        AlbumFragment fragment = new AlbumFragment();
        Bundle args = new Bundle();
        args.putString(ALBUM, album);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            album = getArguments().getString(ALBUM);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ModelProxy.removeStateListener(listener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.inflater = inflater;
        this.view = inflater.inflate(R.layout.fragment_album, container, false);
        ((TextView) view.findViewById(R.id.albumName)).setText(album);

        // add state listener
        listener = new IModel.StateListener() { @Override public void update(final State state) {
            Runnable runUpdate = new Runnable() {
                @Override
                public void run() {
                    updateState(state);
                }
            };
            handler.post(runUpdate);
        }};
        ModelProxy.addStateListener(listener);

        return view;
    }

    private void updateState(State state) {
        List<Song> songs = state.artistSelected.getAlbumSongs(album);
        ((ImageView) view.findViewById(R.id.albumArt)).setImageBitmap(songs.get(0).getArt());

        LinearLayout songsContainer = view.findViewById(R.id.songsContainer);
        for (Song song : songs) {
            View songView = songsContainer.findViewWithTag(song.hash.toString());
            if (songView == null) {
                // Create new song container
                songView = inflateSongContainer(song);
                songsContainer.addView(songView);
            }
            ((TextView) songView.findViewById(R.id.songName)).setText(song.name);
            ((TextView) songView.findViewById(R.id.songDuration)).setText(song.duration());

            int color = state.songPlaying != null && state.songPlaying.hash.toString().equals(songView.getTag())
                    ? Color.parseColor("#03a9f4")
                    : Color.WHITE;
            ((TextView) songView.findViewById(R.id.songName)).setTextColor(color);
            ((TextView) songView.findViewById(R.id.songDuration)).setTextColor(color);
        }
    }

    private View inflateSongContainer(Song song) {
        View songView = inflater.inflate(R.layout.album_song, null);
        songView.setTag(song.hash.toString());
        songView.findViewById(R.id.addToPlaylist).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            System.out.println("* Add song to playlist"); // TODO implement add song to playlist
        }});
        return songView;
    }

}
