package buddybox.ui.library;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import sov.buddybox.R;

import java.util.ArrayList;
import java.util.List;

import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.State;
import buddybox.core.events.PlayPlaylist;
import buddybox.core.events.PlaylistSelected;
import buddybox.ui.ModelProxy;
import buddybox.ui.PlaylistActivity;

import static buddybox.ui.ModelProxy.dispatch;

public class PlaylistsFragment extends Fragment {

    private View view;
    private PlaylistsArrayAdapter playlistsAdapter;
    private List<Playlist> playlists;
    private Playlist playlistPlaying;
    private IModel.StateListener listener;
    private FragmentActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public PlaylistsFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.library_playlists, container, false);

        // List playlists
        ListView list = view.findViewById(R.id.playlists);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new PlaylistSelected(playlists.get(i)));
            startActivity(new Intent(getContext(), PlaylistActivity.class));
        }});
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { @Override public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new PlayPlaylist(playlists.get(i), 0));
            return true;
        }});
        View footer = inflater.inflate(R.layout.list_footer, list, false);
        list.addFooterView(footer);
        playlistsAdapter = new PlaylistsArrayAdapter();
        list.setAdapter(playlistsAdapter);

        // If state was updated before fragment creation
        if (playlists != null)
            updatePlaylists();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
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
    }

    @Override
    public void onDestroy() {
        ModelProxy.removeStateListener(listener);
        super.onDestroy();
    }

    private class PlaylistsArrayAdapter extends ArrayAdapter<Playlist> {

        PlaylistsArrayAdapter() {
            super(activity, -1, new ArrayList<Playlist>());
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView == null
                    ? activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false)
                    : convertView;

            Playlist item = getItem(position);
            if (item == null)
                return rowView;

            TextView text1 = rowView.findViewById(android.R.id.text1);
            TextView text2 = rowView.findViewById(android.R.id.text2);
            text1.setText(item.name());
            text2.setText(item.subtitle());

            if (item == playlistPlaying) {
                text1.setTextColor(Color.parseColor("#03a9f4"));
                text2.setTextColor(Color.parseColor("#03a9f4"));
            } else {
                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.LTGRAY);
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