package buddybox.ui.library;

import android.content.Intent;
import android.graphics.Color;
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

import buddybox.core.IModel;
import buddybox.core.events.Play;
import buddybox.core.Playlist;
import buddybox.core.State;
import buddybox.core.events.PlaylistSelected;
import buddybox.ui.MainActivity;
import buddybox.ui.ModelProxy;
import buddybox.ui.PlayingActivity;
import buddybox.ui.PlaylistActivity;

import static buddybox.ui.ModelProxy.dispatch;

public class PlaylistsFragment extends Fragment {

    private View view;
    private PlaylistsArrayAdapter playlistsAdapter;
    private List<Playlist> playlists;
    private Playlist playlistPlaying;
    private IModel.StateListener listener;

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
            // dispatch(new Play(playlists.get(i), 0));
            dispatch(new PlaylistSelected(playlists.get(i)));
            startActivity(new Intent(getContext(), PlaylistActivity.class));
        }});
        playlistsAdapter = new PlaylistsArrayAdapter();
        list.setAdapter(playlistsAdapter);

        // If state was updated before fragment creation
        if (playlists != null)
            updatePlaylists();

        listener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        ModelProxy.addStateListener(listener);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ModelProxy.removeStateListener(listener);
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
            TextView text1 = (TextView) rowView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) rowView.findViewById(android.R.id.text2);
            text1.setText(item.name());
            text2.setText(String.format("%s %s", item.subtitle(), item.duration()));

            if (item == playlistPlaying) {
                text1.setTextColor(Color.parseColor("#81c784"));
                text2.setTextColor(Color.parseColor("#81c784"));
            } else {
                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.WHITE);
            }

            return rowView;
        }

        void updateState() {
            clear();
            addAll(playlists);
        }
    }

    public void updateState(State state) {
        playlists = state.playlists;
        playlistPlaying = state.playlistPlaying;

        updatePlaylists();
    }

    private void updatePlaylists() {
        playlistsAdapter.updateState();
        if (playlists.isEmpty()) {
            view.findViewById(R.id.playlists_empty).setVisibility(View.VISIBLE);
            view.findViewById(R.id.playlists).setVisibility(View.INVISIBLE);
            return;
        }
        view.findViewById(R.id.playlists_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.playlists).setVisibility(View.VISIBLE);
    }
}