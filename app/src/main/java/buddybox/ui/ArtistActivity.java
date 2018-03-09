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
        System.out.println("* Artist: " + state.artistSelected.name);
        Map<String, List<Song>> map = state.artistSelected.songsByAlbum();
        for (String album : map.keySet()) {
            inflateAlbum(album, map.get(album));
            System.out.println("** Album: " + album);
            for (Song song : map.get(album))
                System.out.println("*** Song: " + song.name);
        }

    }

    private void inflateAlbum(String album, List<Song> songs) {
        LinearLayout albumsContainer = findViewById(R.id.albums_container);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setId(View.generateViewId());

        getFragmentManager().beginTransaction().add(ll.getId(), AlbumFragment.newInstance(album)).commit();

        albumsContainer.addView(ll);
    }
}
