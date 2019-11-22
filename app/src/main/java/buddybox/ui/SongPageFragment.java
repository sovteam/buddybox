package buddybox.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.ArtistSelectedByName;
import buddybox.core.events.PlaylistSelected;
import buddybox.ui.util.AsyncImage2;
import sov.buddybox.R;

import static buddybox.ui.ModelProxy.dispatch;

public class SongPageFragment extends Fragment {

    private IModel.StateListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private State lastState;
    private int position;
    private ViewGroup rootView;
    private Song song;
    private ImageView pageArt;
    private TextView name;
    private TextView artist;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_song_page, container, false);
        if (getArguments() == null)
            return rootView;

        position = getArguments().getInt("position");
        rootView.findViewById(R.id.playingSongArtist).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            dispatch(new ArtistSelectedByName(song.artist));
            startActivity(new Intent(getContext(), ArtistActivity.class));
        }});

        pageArt = rootView.findViewById(R.id.pageArt);
        pageArt.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            if (lastState.playlistPlaying.getClass() == Playlist.class && lastState.playlistPlaying.getId() != 0L) {
                dispatch(new PlaylistSelected(lastState.playlistPlaying));
                startActivity(new Intent(getContext(), PlaylistActivity.class));
            } else {
                dispatch(new ArtistSelectedByName(lastState.songPlaying.artist));
                startActivity(new Intent(getContext(), ArtistActivity.class));
            }
        }});

        name = rootView.findViewById(R.id.playingSongName);
        artist = rootView.findViewById(R.id.playingSongArtist);

        return rootView;
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

    private void updateState(State state) {
        if (state.playlistPlaying == null || position >= state.playlistPlaying.size())
            return;

        song = state.playlistPlaying.song(position, state.isShuffle);
        name.setText(song.name);
        artist.setText(song.artist);

        int color = song.isMissing
                ? Color.parseColor("#e53935")
                : Color.WHITE;
        name.setTextColor(color);
        artist.setTextColor(color);

        AsyncImage2.setImage(pageArt, song, R.mipmap.sneer2);

        lastState = state;
    }
}
