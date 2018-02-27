package buddybox.ui.library;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.State;
import buddybox.ui.ModelProxy;

public class ArtistsFragment extends Fragment {

    private ArtistsArrayAdapter artistsAdapter;
    private View view;
    private List<Artist> artists;
    private IModel.StateListener listener;
    private FragmentActivity activity;
    private State lastState;

    public ArtistsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.library_artists, container, false);

        // List artists
        ListView list = view.findViewById(R.id.artists);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            System.out.println("GUI Artist click " + i);
        }});
        View footer = inflater.inflate(R.layout.list_footer, list, false);
        list.addFooterView(footer);
        artistsAdapter = new ArtistsArrayAdapter();
        list.setAdapter(artistsAdapter);

        if (artists != null)
            updateArtists();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ModelProxy.isInitialized()) {
            listener = new IModel.StateListener() { @Override public void update(State state) {
                updateState(state);
            }};
            ModelProxy.addStateListener(listener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ModelProxy.removeStateListener(listener);
    }

    private class ArtistsArrayAdapter extends ArrayAdapter<Artist> {
        ArtistsArrayAdapter() {
            super(activity, -1, new ArrayList<Artist>());
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = (convertView == null)
                    ? activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false)
                    : convertView;

            Artist artist = getItem(position);
            if (artist == null)
                return rowView;

            setText(rowView, android.R.id.text1, artist.name);
            setText(rowView, android.R.id.text2, Integer.toString(artist.songsCount()) + " songs");

            return rowView;
        }

        private void setText(View rowView, int id, String value) {
            TextView textView = rowView.findViewById(id);
            textView.setText(value);
        }

        void update(List<Artist> artists) {
            clear();
            addAll(artists);
        }
    }

    public void updateState(State state) {
        artists = state.artists;
        if (artists == null)
            return;

        Collections.sort(artists, new Comparator<Artist>() { @Override public int compare(Artist artistA, Artist artistB) {
            return artistA.name.compareTo(artistB.name);
        }});

        updateArtists(state);
        lastState = state;
    }

    private void updateArtists() {
        if (lastState != null)
            updateArtists(lastState);
    }

    private void updateArtists(State state) {
        artistsAdapter.update(artists);
        if (state.syncLibraryPending) {
            view.findViewById(R.id.footerLoading).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.footerLoading).setVisibility(View.GONE);
            if (artists.isEmpty()) {
                view.findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
                view.findViewById(R.id.artists).setVisibility(View.INVISIBLE);
                return;
            }
        }
        view.findViewById(R.id.library_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.artists).setVisibility(View.VISIBLE);
    }
}
