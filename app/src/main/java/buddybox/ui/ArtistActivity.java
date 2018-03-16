package buddybox.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
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
import static buddybox.core.events.Play.SHUFFLE_PLAY_ARTIST;

public class ArtistActivity extends AppCompatActivity {

    private IModel.StateListener listener;
    private boolean isBioCollapsed = true;

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

        final TextView content = findViewById(R.id.bioContent);
        findViewById(R.id.bioCollapse).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            isBioCollapsed = !isBioCollapsed;
            if (isBioCollapsed) {
                ObjectAnimator animation = ObjectAnimator.ofInt(content,"maxLines",3);
                animation.setDuration(500);
                animation.start();
                ((ImageView) findViewById(R.id.bioCollapse)).setImageResource(R.drawable.ic_expand_more);
            } else {
                ObjectAnimator animation = ObjectAnimator.ofInt(content,"maxLines",content.getLineCount());
                animation.setDuration(500);
                animation.start();
                ((ImageView) findViewById(R.id.bioCollapse)).setImageResource(R.drawable.ic_expand_less);
            }
        }});

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
        ((TextView)findViewById(R.id.artistName)).setText(state.artistSelected.name);
        ((TextView)findViewById(R.id.artistSongsCount)).setText(state.artistSelected.songsCountPrint());

        if (state.artistSelected.bio == null) {
            findViewById(R.id.artistBio).setVisibility(View.GONE);
        } else {
            findViewById(R.id.artistBio).setVisibility(View.VISIBLE);
            String bio = state.artistSelected.bio.replace("\n", "<br />");
            ((TextView) findViewById(R.id.bioContent)).setText(Html.fromHtml(bio));
        }

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
