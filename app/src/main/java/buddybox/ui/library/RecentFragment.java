package buddybox.ui.library;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.ArrayList;
import java.util.List;

import buddybox.core.Album;
import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Playable;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;
import buddybox.ui.MainActivity;
import buddybox.ui.ModelProxy;
import buddybox.ui.PlayingActivity;
import buddybox.ui.library.dialogs.SelectPlaylistDialogFragment;

import static buddybox.model.Model.ALL_SONGS;
import static buddybox.ui.ModelProxy.dispatch;

public class RecentFragment extends Fragment {

    private PlayablesArrayAdapter playables;
    private View view;
    private IModel.StateListener listener;
    private FragmentActivity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private State lastState;

    public RecentFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.library_recent, container, false);

        // List recent songs
        ListView list = view.findViewById(R.id.recentPlayables);
        View footer = inflater.inflate(R.layout.list_footer, list, false);
        list.addFooterView(footer);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            startActivity(new Intent(getContext(), PlayingActivity.class));
            dispatch(new Play(lastState.recent.get(i)));
        }});
        playables = new PlayablesArrayAdapter();
        list.setAdapter(playables);

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

    private class PlayablesArrayAdapter extends ArrayAdapter<Playable> {
        PlayablesArrayAdapter() {
            super(activity, -1, new ArrayList<Playable>());
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            Playable playable = getItem(position);
            View rowView = convertView == null
                    ? activity.getLayoutInflater().inflate(R.layout.playable_item, parent, false)
                    : convertView;

            if (playable == null)
                return rowView;

            TextView name = rowView.findViewById(R.id.name);
            TextView subtitle = rowView.findViewById(R.id.subtitle);
            name.setText(playable.name());
            subtitle.setText(playable.subtitle());

            // playlist playing or song playing
            if (playable == lastState.playlistPlaying || (lastState.songPlaying == playable && lastState.playlistPlaying.name().equals(ALL_SONGS))) {
                name.setTextColor(Color.parseColor("#4fc3f7"));
                subtitle.setTextColor(Color.parseColor("#4fc3f7"));
            } else {
                name.setTextColor(Color.WHITE);
                subtitle.setTextColor(Color.LTGRAY);
            }

            if (playable.getClass() == Song.class)
                updateSongItem((Song) playable, rowView);
            else
                updatePlayableItem(playable, rowView);

            return rowView;
        }

        private void updatePlayableItem(Playable playable, View rowView) {
            rowView.findViewById(R.id.addToPlaylist).setVisibility(View.GONE);
            if (playable.getClass() == Artist.class) {
                int icon = playable == lastState.playlistPlaying
                        ? R.drawable.ic_person_blue
                        : R.drawable.ic_person;
                ((ImageView)rowView.findViewById(R.id.playableArt)).setImageResource(icon);
            }
            if (playable.getClass() == Playlist.class) {
                int icon = playable == lastState.playlistPlaying
                        ? R.drawable.ic_queue_music_blue
                        : R.drawable.ic_queue_music;
                ((ImageView)rowView.findViewById(R.id.playableArt)).setImageResource(icon);
            }
            if (playable.getClass() == Album.class) {
                int icon = playable == lastState.playlistPlaying
                        ? R.drawable.ic_library_music_blue
                        : R.drawable.ic_library_music_grey;
                ((ImageView)rowView.findViewById(R.id.playableArt)).setImageResource(icon);
            }
        }

        private void updateSongItem(final Song song, View rowView) {
            ImageView addToPlaylist = rowView.findViewById(R.id.addToPlaylist);
            addToPlaylist.setVisibility(View.VISIBLE);
            addToPlaylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openSelectPlaylistDialog(song);
                }
            });

            Bitmap art = song.getArt();
            if (art != null)
                ((ImageView) rowView.findViewById(R.id.playableArt)).setImageBitmap(art);
            else
                ((ImageView) rowView.findViewById(R.id.playableArt)).setImageResource(R.mipmap.sneer2);
        }

        void updateState(State state) {
            clear();
            addAll(state.recent);
        }
    }

    private void openSelectPlaylistDialog(Song song) {
        SelectPlaylistDialogFragment frag = new SelectPlaylistDialogFragment();

        // Select playlists song is not associated
        List<Playlist> playlistsForSong = new ArrayList<>();
        for (Playlist playlist : lastState.playlists){
            if (!playlist.hasSong(song))
                playlistsForSong.add(playlist);
        }

        // Get playlists info from bundle
        long[] playlistIds = new long[playlistsForSong.size()];
        ArrayList<String> playlistNames = new ArrayList<>();
        for (int i = 0; i < playlistsForSong.size(); i++) {
            Playlist playlist = playlistsForSong.get(i);
            playlistIds[i] = playlist.getId();
            playlistNames.add(playlist.name);
        }

        Bundle args = new Bundle();
        args.putString("songHash", song.hash.toString());
        args.putStringArrayList("playlistsNames", playlistNames);
        args.putLongArray("playlistsIds", playlistIds);
        frag.setArguments(args);

        FragmentManager fragManager = getFragmentManager();
        if (fragManager != null)
            frag.show(fragManager, "Select Playlist");
    }

    public void updateState(State state) {
        playables.updateState(state);

        // show/hide footers
        if (state.syncLibraryPending) {
            view.findViewById(R.id.footerLoading).setVisibility(View.VISIBLE);
            view.findViewById(R.id.library_empty).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.footerLoading).setVisibility(View.GONE);
            if (state.recent.isEmpty()) {
                view.findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
                view.findViewById(R.id.recentPlayables).setVisibility(View.INVISIBLE);
                return;
            }
        }
        view.findViewById(R.id.library_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.recentPlayables).setVisibility(View.VISIBLE);

        lastState = state;
    }
}
