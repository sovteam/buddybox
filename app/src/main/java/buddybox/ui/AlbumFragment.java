package buddybox.ui;

import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;

public class AlbumFragment extends Fragment {

    private static final String ALBUM = "album";
    private String album;

    private View view;
    private IModel.StateListener listener;
    private LayoutInflater inflater;

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
        listener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        ModelProxy.addStateListener(listener);

        return view;
    }

    private void updateState(State state) {
        List<Song> songs = state.artistSelected.getAlbumSongs(album);
        ((ImageView) view.findViewById(R.id.albumArt)).setImageBitmap(songs.get(0).getArt());

        // TODO optimize add/remove
        LinearLayout songsContainer = view.findViewById(R.id.songsContainer);
        songsContainer.removeAllViews();
        for (Song song : songs) {
            View view = inflater.inflate(R.layout.album_song, null);
            //view.setId(song.hashCode());
            ((TextView) view.findViewById(R.id.songName)).setText(song.name);
            ((TextView) view.findViewById(R.id.songDuration)).setText(song.duration());
            view.findViewById(R.id.addToPlaylist).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println("* Add song to playlist");
                }
            });
            songsContainer.addView(view);
        }
    }

}
