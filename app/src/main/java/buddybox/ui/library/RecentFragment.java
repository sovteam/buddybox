package buddybox.ui.library;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.adalbertosoares.buddybox.R;

import java.util.ArrayList;
import java.util.List;

import buddybox.api.Play;
import buddybox.api.Playable;
import buddybox.api.Playlist;
import buddybox.api.Song;
import buddybox.api.VisibleState;
import buddybox.ui.MainActivity;

import static buddybox.CoreSingleton.dispatch;

public class RecentFragment extends Fragment {

    private Playlist recentPlaylist;
    private PlayablesArrayAdapter playables;
    private View view;
    private List<Playlist> playlists;

    public RecentFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.library_recent, container, false);

        // List recent songs
        ListView list = (ListView) view.findViewById(R.id.recentPlayables);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new Play(recentPlaylist, i));
        }});
        playables = new PlayablesArrayAdapter();
        list.setAdapter(playables);

        // If state was updated before fragment creation
        if (recentPlaylist != null)
            updatePlaylist();

        System.out.println(">>>>>> recent frag created. UpdatePlaylist: " + (recentPlaylist != null));

        return view;
    }

    private class PlayablesArrayAdapter extends ArrayAdapter<Playable> {
        PlayablesArrayAdapter() {
            super(getActivity(), -1, new ArrayList<Playable>());
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View rowView = convertView == null
                    ? getActivity().getLayoutInflater().inflate(R.layout.song_item, parent, false)
                    : convertView;

            Playable item = getItem(position);
            setText(rowView, R.id.text1, item.name());
            setText(rowView, R.id.text2, item.subtitle() + " " + item.duration()); // TODO remove duration

            rowView.findViewById(R.id.addToPlaylist).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.println(">>>>>>>>>>>>>>> add to playlist " + view);
                    openSelectPlaylistDialog(recentPlaylist.songs.get(position));
                }
            });

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

    private void openSelectPlaylistDialog(Song song) {
        SelectPlaylistDialogFragment frag = new SelectPlaylistDialogFragment();

        //TODO remove playlists that song already belongs
        ArrayList<String> list = new ArrayList<>();
        for (Playlist playlist : playlists) {
            list.add(playlist.name);
        }

        Bundle args = new Bundle();
        args.putString("songId", song.name); // TODO switch to songId
        args.putStringArrayList("playlists", list);
        frag.setArguments(args);

        frag.show(getFragmentManager(), "Select Playlist");
    }

    public void updateState(VisibleState state) {
        System.out.println(">>>>>> update state. has playables " + (playables != null));
        recentPlaylist = state.recentPlaylist;
        playlists = state.playlists;

        if (playables == null)
            return;

        updatePlaylist();
    }

    private void updatePlaylist() {
        System.out.println("## updatePlaylist" + recentPlaylist.songs.size());
        playables.updateRecent(recentPlaylist);
        if (recentPlaylist.songs.isEmpty()) {
            view.findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
            view.findViewById(R.id.recentPlayables).setVisibility(View.INVISIBLE);
            return;
        }
        view.findViewById(R.id.library_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.recentPlayables).setVisibility(View.VISIBLE);
    }
}
