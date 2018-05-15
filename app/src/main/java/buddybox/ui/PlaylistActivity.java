package buddybox.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragListView;
import com.woxthebox.draglistview.swipe.ListSwipeHelper.OnSwipeListenerAdapter;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.PlaylistChangeSongPosition;
import buddybox.core.events.PlaylistRemoveSong;
import sov.buddybox.R;

import static buddybox.core.events.Play.SHUFFLE_PLAY;
import static buddybox.ui.ModelProxy.dispatch;

public class PlaylistActivity extends AppCompatActivity {

    private IModel.StateListener listener;
    private PlaylistSongsAdapter songsAdapter;
    private Playlist playlist;
    private DragListView mDragListView;
    private boolean dragging = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        // set events
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            finish();
        }});
        findViewById(R.id.playlistMore).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { openPlaylistOptionsDialog(); }});
        findViewById(R.id.shufflePlay).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { dispatch(SHUFFLE_PLAY); }});

        MySwipeRefreshLayout mRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mRefreshLayout.setEnabled(false);
        mRefreshLayout.setVerticalScrollBarEnabled(false);

        songsAdapter = new PlaylistSongsAdapter(PlaylistActivity.this, R.layout.playlist_song_item, R.id.drag, false);
        mDragListView = findViewById(R.id.drag_list_view);
        mDragListView.setAdapter(songsAdapter, true);
        mDragListView.setCanDragHorizontally(false);
        mDragListView.setCustomDragItem(new MyDragItem(PlaylistActivity.this, R.layout.list_item));
        mDragListView.setLayoutManager(new LinearLayoutManager(PlaylistActivity.this));
        mDragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragStarted(int position) {
                dragging = true;
            }

            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (fromPosition != toPosition) {
                    dispatch(new PlaylistChangeSongPosition(playlist, fromPosition, toPosition));
                }
                dragging = false;
            }
        });

        mDragListView.setSwipeListener(new OnSwipeListenerAdapter() { @Override public void onItemSwipeStarted(ListSwipeItem item) { } @Override public void onItemSwipeEnded(ListSwipeItem item, ListSwipeItem.SwipeDirection swipedDirection) {
            // Swipe to delete on right
            if (swipedDirection == ListSwipeItem.SwipeDirection.RIGHT) {
                Pair<Long, Song> adapterItem = (Pair<Long, Song>) item.getTag();
                final int pos = mDragListView.getAdapter().getPositionForItem(adapterItem);
                mDragListView.getAdapter().removeItem(pos);
                dispatch(new PlaylistRemoveSong(playlist, adapterItem.second));
            }
        }});

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

    private static class MyDragItem extends DragItem {

        MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }
        @Override
        public void onBindDragView(View clickedView, View dragView) {
            ImageView albumArt = clickedView.findViewById(R.id.albumArt);
            Bitmap bitmap = ((BitmapDrawable)albumArt.getDrawable()).getBitmap();
            if (bitmap != null)
                ((ImageView) dragView.findViewById(R.id.albumArt)).setImageBitmap(bitmap);
            else
                ((ImageView) dragView.findViewById(R.id.albumArt)).setImageResource(R.mipmap.sneer2);

            CharSequence name = ((TextView) clickedView.findViewById(R.id.songName)).getText();
            ((TextView) dragView.findViewById(R.id.songName)).setText(name);
            CharSequence artist = ((TextView) clickedView.findViewById(R.id.songArtist)).getText();
            ((TextView) dragView.findViewById(R.id.songArtist)).setText(artist);
            dragView.findViewById(R.id.item_layout).setBackgroundColor(Color.parseColor("#1976d2"));
        }
    }

    private void updateState(State state) {
        playlist = state.selectedPlaylist;
        if (playlist == null) {
            finish();
            return;
        }

        if (!dragging)
            songsAdapter.updateState(state);

        ((TextView)findViewById(R.id.playlistName)).setText(playlist.name());
        ((TextView)findViewById(R.id.playlistSubtitle)).setText(playlist.subtitle());
    }

    private void openPlaylistOptionsDialog() {
        PlaylistOptionsDialog dialog = new PlaylistOptionsDialog();
        Bundle args = new Bundle();
        args.putLong("playlistId", playlist.getId());
        args.putString("playlistName", playlist.name);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "Playlist Options");
    }
}
