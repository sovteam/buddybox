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

import com.adalbertosoares.buddybox.R;

import java.util.ArrayList;
import java.util.List;

import buddybox.api.Playlist;
import buddybox.api.State;

public class PlaylistsFragment extends Fragment {

    private View view;
    private PlaylistsArrayAdapter playlistsAdapter;
    private List<Playlist> playlists;

    public PlaylistsFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.library_playlists, container, false);

        // List playlists
        ListView list = (ListView) view.findViewById(R.id.playlists);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            System.out.println(">>> Play playlist");
            //dispatch(new Play(playlists.song(0), i));
        }});
        playlistsAdapter = new PlaylistsArrayAdapter();
        list.setAdapter(playlistsAdapter);

        // If state was updated before fragment creation
        if (playlists != null)
            updatePlaylists();

        System.out.println(">>>>>> playlists frag created. UpdatePlaylist: " + (playlists != null));

        return view;
    }

    private class PlaylistsArrayAdapter extends ArrayAdapter<Playlist> {
        PlaylistsArrayAdapter() {
            super(getActivity(), -1, new ArrayList<Playlist>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView == null
                    ? getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false)
                    : convertView;

            Playlist item = getItem(position);
            setText(rowView, android.R.id.text1, item.name());
            setText(rowView, android.R.id.text2, item.subtitle() + " - " + item.duration()); // TODO remove duration

            return rowView;
        }

        private void setText(View rowView, int id, String value) {
            TextView textView = (TextView) rowView.findViewById(id);
            textView.setText(value);
        }

        void update(List<Playlist> playlists) {
            clear();
            addAll(playlists);
        }

    }

    public void updateState(State state) {
        System.out.println(">>>>>> update state. has playables " + (playlistsAdapter != null));
        playlists = state.playlists;
        if (playlistsAdapter == null)
            return;

        updatePlaylists();
    }

    private void updatePlaylists() {
        System.out.println("## :/ updatePlaylists " + playlists.size());
        playlistsAdapter.update(playlists);
        if (playlists.isEmpty()) {
            view.findViewById(R.id.playlists_empty).setVisibility(View.VISIBLE);
            view.findViewById(R.id.playlists).setVisibility(View.INVISIBLE);
            return;
        }
        view.findViewById(R.id.playlists_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.playlists).setVisibility(View.VISIBLE);
    }
}