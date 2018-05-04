package buddybox.ui.library;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import sov.buddybox.R;

import java.util.ArrayList;
import java.util.List;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.State;
import buddybox.core.events.ArtistSelected;
import buddybox.ui.ArtistActivity;
import buddybox.ui.ModelProxy;

import static buddybox.ui.ModelProxy.dispatch;

public class ArtistsFragment extends Fragment {

    private ArtistsArrayAdapter artistsAdapter;
    private View view;
    private IModel.StateListener listener;
    private FragmentActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
                    ? activity.getLayoutInflater().inflate(R.layout.artist_item, parent, false)
                    : convertView;

            final Artist artist = getItem(position);
            if (artist == null)
                return rowView;

            setText(rowView, R.id.artistName, artist.name);
            setText(rowView, R.id.artistSongs, Integer.toString(artist.size()) + " songs");
            if (artist.picture == null) {
                ((ImageView)rowView.findViewById(R.id.artistPicture)).setImageResource(R.drawable.ic_person);
            } else {
                ((ImageView)rowView.findViewById(R.id.artistPicture)).setImageBitmap(artist.picture);
            }

            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                dispatch(new ArtistSelected(artist));
                startActivity(new Intent(getContext(), ArtistActivity.class));
                }
            });

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
        artistsAdapter.update(state.artists);
        if (state.syncLibraryPending) {
            view.findViewById(R.id.footerLoading).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.footerLoading).setVisibility(View.GONE);
            if (state.artists.isEmpty()) {
                view.findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
                view.findViewById(R.id.artists).setVisibility(View.INVISIBLE);
                return;
            }
        }
        view.findViewById(R.id.library_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.artists).setVisibility(View.VISIBLE);
    }
}
