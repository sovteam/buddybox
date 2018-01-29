package buddybox.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
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
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;
import buddybox.ui.library.dialogs.SelectPlaylistDialogFragment;

import static buddybox.ui.ModelProxy.dispatch;

public class PlaylistActivity extends AppCompatActivity {

    private IModel.StateListener listener;
    private PlaylistSongsAdapter songsAdapter;
    private Playlist playlist;
    private Song songPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);

        // set events
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            finish();
        }});
        findViewById(R.id.playlistMore).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { openPlaylistOptionsDialog(); }});

        // list playlist songs
        ListView list = (ListView) findViewById(R.id.playlistSongs);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            dispatch(new Play(playlist, i));
        }});
        songsAdapter = new PlaylistSongsAdapter();
        list.setAdapter(songsAdapter);

        // set listener
        listener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        ModelProxy.addStateListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ModelProxy.removeStateListener(listener);
    }

    private class PlaylistSongsAdapter extends ArrayAdapter<Song> {

        PlaylistSongsAdapter() {
            super(PlaylistActivity.this, -1, new ArrayList<Song>());
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView == null
                    ? getLayoutInflater().inflate(R.layout.playlist_song_item, parent, false)
                    : convertView;

            Song item = getItem(position);
            TextView text1 = (TextView) rowView.findViewById(R.id.songName);
            TextView text2 = (TextView) rowView.findViewById(R.id.songSubtitle);
            text1.setText(item.name());
            text2.setText(item.subtitle());

            if (item == songPlaying) {
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
            addAll(playlist.songs);
        }
    }

    private void updateState(State state) {
        playlist = state.selectedPlaylist;
        songPlaying = state.songPlaying;
        if (playlist == null) {
            finish();
            return;
        }

        songsAdapter.updateState();
        ((TextView)findViewById(R.id.playlistName)).setText(playlist.name());
        ((TextView)findViewById(R.id.playlistSubtitle)).setText(playlist.subtitle());
    }

    private void openPlaylistOptionsDialog() {
        PlaylistOptionsDialog frag = new PlaylistOptionsDialog();

        Bundle args = new Bundle();
        args.putLong("playlistId", playlist.id);
        frag.setArguments(args);
        frag.show(getSupportFragmentManager(), "Playlist Options");
    }
}
