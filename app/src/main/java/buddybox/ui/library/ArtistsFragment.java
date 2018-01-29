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

    public ArtistsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.library_artists, container, false);

        // List artists
        ListView list = (ListView) view.findViewById(R.id.artists);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            System.out.println("GUI Artist click " + i);
        }});
        artistsAdapter = new ArtistsArrayAdapter();
        list.setAdapter(artistsAdapter);

        if (artists != null)
            updateArtists();

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

    private class ArtistsArrayAdapter extends ArrayAdapter<Artist> {
        ArtistsArrayAdapter() {
            super(getActivity(), -1, new ArrayList<Artist>());
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View rowView = convertView == null
                    ? getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false)
                    : convertView;

            Artist artist = getItem(position);
            setText(rowView, android.R.id.text1, artist.name);
            setText(rowView, android.R.id.text2, Integer.toString(artist.songsCount()) + " songs");

            return rowView;
        }

        private void setText(View rowView, int id, String value) {
            TextView textView = (TextView) rowView.findViewById(id);
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

        updateArtists();
    }

    private void updateArtists() {
        artistsAdapter.update(artists);
        if (artists.isEmpty()) {
            view.findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
            view.findViewById(R.id.artists).setVisibility(View.INVISIBLE);
            return;
        }
        view.findViewById(R.id.library_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.artists).setVisibility(View.VISIBLE);
    }
}
