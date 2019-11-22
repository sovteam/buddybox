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

import buddybox.ui.util.AsyncImage2;
import sov.buddybox.R;

import buddybox.core.Album;
import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;
import buddybox.core.events.PlayPlaylist;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.SHUFFLE_PLAY_ARTIST;

public class AlbumFragment extends Fragment {

    private static final String ALBUM = "albumName";
    private String albumName;

    private View view;
    private IModel.StateListener listener;
    private LayoutInflater inflater;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Album album;
    private Artist artist;

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
            albumName = getArguments().getString(ALBUM);
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
        ((TextView) view.findViewById(R.id.albumName)).setText(albumName);

        view.findViewById(R.id.albumShufflePlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // todo shuffle play albumName event
                dispatch(SHUFFLE_PLAY_ARTIST);
            }
        });
        view.findViewById(R.id.albumPlayAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatch(new Play(album));
            }
        });

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
        album = state.artistAlbums.get(albumName);
        artist = state.artistSelected;

        Song firstAlbumSong = album.song(0);
        ImageView artView = view.findViewById(R.id.albumArt);
        AsyncImage2.setImage(artView, firstAlbumSong, R.mipmap.sneer2);

        LinearLayout songsContainer = view.findViewById(R.id.songsContainer);
        for (Song song : album.songs) {
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

    private View inflateSongContainer(final Song song) {
        View songView = inflater.inflate(R.layout.album_song, null);
        songView.setTag(song.hash.toString());
        songView.findViewById(R.id.addToPlaylist).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            System.out.println("* Add song to playlist"); // TODO implement add song to playlist
        }});
        songView.findViewById(R.id.songInfoContainer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatch(new PlayPlaylist(artist, artist.indexOf(song, false)));
            }
        });
        return songView;
    }

}
