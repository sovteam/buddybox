package buddybox.ui;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import sov.buddybox.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import buddybox.core.Album;
import buddybox.core.IModel;
import buddybox.core.State;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.SHUFFLE_PLAY_ARTIST;

public class ArtistActivity extends AppCompatActivity {

    private IModel.StateListener listener;
    private boolean isBioCollapsed = true;
    private final Handler handler = new Handler(Looper.getMainLooper());

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


    private void updateState(final State state) {
        if (state.artistSelected == null) {
            finish();
            return;
        }

        // artist details
        if (state.artistSelected.picture == null) {
            ((ImageView)findViewById(R.id.artistPicture)).setImageResource(R.drawable.ic_person);
        } else {
            ((ImageView)findViewById(R.id.artistPicture)).setImageBitmap(state.artistSelected.picture);
        }

        ((TextView)findViewById(R.id.artistName)).setText(state.artistSelected.name);
        ((TextView)findViewById(R.id.artistSongsCount)).setText(state.artistSelected.songsCountPrint());

        if (state.artistSelected.getBio() == null) {
            findViewById(R.id.artistBio).setVisibility(View.GONE);
        } else {
            findViewById(R.id.artistBio).setVisibility(View.VISIBLE);
            String bio = state.artistSelected.getBio().replace("\n", "<br />");
            TextView bioView = findViewById(R.id.bioContent);
            bioView.setText(Html.fromHtml(bio));
            bioView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        // songs by album
        LinearLayout albumsContainer = findViewById(R.id.albums_container);
        List<Album> albums = new ArrayList<>(state.artistAlbums.values());
        Collections.sort(albums, new Comparator<Album>() {
            @Override
            public int compare(Album a1, Album a2) {
                return a1.name.compareTo(a2.name); // todo: or year?
            }
        });
        for (Album album : albums) {
            LinearLayout albumView = albumsContainer.findViewWithTag(album.name);
            if (albumView == null) {
                albumView = inflateAlbum(album.name);
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
