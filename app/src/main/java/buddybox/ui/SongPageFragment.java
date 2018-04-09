package buddybox.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.adalbertosoares.buddybox.R;

import buddybox.core.Artist;
import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.State;
import buddybox.core.events.ArtistSelected;
import buddybox.core.events.PlaylistSelected;

import static buddybox.ui.ModelProxy.dispatch;

public class SongPageFragment extends Fragment {

    private IModel.StateListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private State lastState;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_song_slide_page, container, false);
        ImageView pageArt = rootView.findViewById(R.id.pageArt);

        // TODO move to updateState?
        if (getArguments() != null) {
            byte[] art = getArguments().getByteArray("art");
            if (art != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(art, 0, art.length);
                pageArt.setImageBitmap(bmp);
            }
        } else {
            pageArt.setImageResource(R.mipmap.sneer2);
        }

        pageArt.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            if (lastState.playlistPlaying.getClass() == Artist.class) {
                dispatch(new ArtistSelected((Artist) lastState.playlistPlaying));
                startActivity(new Intent(getContext(), ArtistActivity.class));
            } else if (lastState.playlistPlaying.getClass() == Playlist.class && lastState.playlistPlaying.getId() != 0L) {
                dispatch(new PlaylistSelected(lastState.playlistPlaying));
                startActivity(new Intent(getContext(), PlaylistActivity.class));
            }
        }});

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
        lastState = state;
    }
}
