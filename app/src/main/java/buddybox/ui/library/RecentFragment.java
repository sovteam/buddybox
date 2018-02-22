package buddybox.ui.library;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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
import buddybox.core.Playable;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;
import buddybox.core.events.SongSelected;
import buddybox.ui.EditSongActivity;
import buddybox.ui.ModelProxy;
import buddybox.ui.library.dialogs.SelectPlaylistDialogFragment;

import static buddybox.ui.ModelProxy.dispatch;

public class RecentFragment extends Fragment {

    private Playlist recentPlaylist;
    private PlayablesArrayAdapter playables;
    private View view;
    private List<Playlist> playlists;
    private Song songPlaying;
    private IModel.StateListener listener;
    private FragmentActivity activity;

    public RecentFragment(){}

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
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { @Override public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
            dispatch(new SongSelected(recentPlaylist.song(pos).hash.toString()));
            startActivity(new Intent(getContext(), EditSongActivity.class));
            return true;
        }});
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new Play(recentPlaylist, i));
        }});
        View footer = inflater.inflate(R.layout.list_footer, list, false);
        list.addFooterView(footer);
        playables = new PlayablesArrayAdapter();
        list.setAdapter(playables);

        // If state was updated before fragment creation
        if (recentPlaylist != null)
            updatePlaylist();

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

    private class PlayablesArrayAdapter extends ArrayAdapter<Playable> {
        PlayablesArrayAdapter() {
            super(activity, -1, new ArrayList<Playable>());
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView == null
                    ? activity.getLayoutInflater().inflate(R.layout.song_item, parent, false)
                    : convertView;

            Playable item = getItem(position);
            if (item == null)
                return rowView;

            TextView text1 = rowView.findViewById(R.id.songName);
            TextView text2 = rowView.findViewById(R.id.text2);
            text1.setText(item.name());
            text2.setText(item.subtitle());

            if (item == songPlaying) {
                text1.setTextColor(Color.parseColor("#4fc3f7"));
                text2.setTextColor(Color.parseColor("#4fc3f7"));
            } else {
                text1.setTextColor(Color.WHITE);
                text2.setTextColor(Color.LTGRAY);
            }

            rowView.findViewById(R.id.songMore).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openSelectPlaylistDialog(recentPlaylist.songs.get(position));
                }
            });

            return rowView;
        }

        void updateState() {
            clear();
            addAll(recentPlaylist.songs);
        }
    }

    private void openSelectPlaylistDialog(Song song) {
        SelectPlaylistDialogFragment frag = new SelectPlaylistDialogFragment();

        // Select playlists song is not associated
        List<Playlist> playlistsForSong = new ArrayList<>();
        for (Playlist playlist : playlists){
            if (!playlist.hasSong(song))
                playlistsForSong.add(playlist);
        }

        // Get playlists infos to bundle
        long[] playlistIds = new long[playlistsForSong.size()];
        ArrayList<String> playlistNames = new ArrayList<>();
        for (int i = 0; i < playlistsForSong.size(); i++) {
            Playlist playlist = playlistsForSong.get(i);
            playlistIds[i] = playlist.id;
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
        songPlaying = state.songPlaying;
        recentPlaylist = state.allSongsPlaylist;
        playlists = state.playlists;

        updatePlaylist();
    }

    private void updatePlaylist() {
        playables.updateState();
        if (recentPlaylist.songs.isEmpty()) {
            view.findViewById(R.id.library_empty).setVisibility(View.VISIBLE);
            view.findViewById(R.id.recentPlayables).setVisibility(View.INVISIBLE);
            return;
        }
        view.findViewById(R.id.library_empty).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.recentPlayables).setVisibility(View.VISIBLE);
    }
}
