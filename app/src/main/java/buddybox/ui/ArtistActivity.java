package buddybox.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.adalbertosoares.buddybox.R;

import java.util.List;
import java.util.Map;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SHUFFLE_PLAY_ARTIST;

public class ArtistActivity extends AppCompatActivity {

    private IModel.StateListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        findViewById(R.id.shufflePlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            dispatch(SHUFFLE_PLAY_ARTIST);
            }
        });

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


    private void updateState(State state) {
        if (state.artistSelected == null) {
            finish();
            return;
        }

        // artist details
        ((ImageView)findViewById(R.id.picture)).setImageBitmap(state.artistSelected.picture);
        ((TextView)findViewById(R.id.songName)).setText(state.artistSelected.name);
        ((TextView)findViewById(R.id.songDuration)).setText(state.artistSelected.songsCountPrint());

        // songs by album
        Map<String, List<Song>> map = state.artistSelected.songsByAlbum();
        LinearLayout albumsContainer = findViewById(R.id.albums_container);

        for (String album : map.keySet()) {
            LinearLayout albumView = albumsContainer.findViewWithTag(album);
            if (albumView == null) {
                albumView = inflateAlbum(album);
                albumsContainer.addView(albumView);
            }
        }
    }

    private LinearLayout inflateAlbum(String album) {
        LinearLayout albumView = new LinearLayout(this);
        albumView.setTag(album);
        albumView.setOrientation(LinearLayout.HORIZONTAL);
        albumView.setId(View.generateViewId());
        getFragmentManager().beginTransaction().add(albumView.getId(), AlbumFragment.newInstance(album)).commit();
        return albumView;
    }
}
