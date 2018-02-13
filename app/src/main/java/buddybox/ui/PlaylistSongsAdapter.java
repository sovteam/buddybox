package buddybox.ui;

import android.graphics.Color;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.adalbertosoares.buddybox.R;
import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;

import static buddybox.ui.ModelProxy.dispatch;

class PlaylistSongsAdapter extends DragItemAdapter<Pair<Long, Song>, PlaylistSongsAdapter.ViewHolder> {

    private Playlist playlist;
    private Song songPlaying;
    private final int mLayoutId;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;

    PlaylistSongsAdapter(int layoutId, int grabHandleId, boolean dragOnLongPress) {
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Song song = mItemList.get(position).second;
        holder.setSong(song);
        holder.songName.setText(song.name);
        holder.songArtist.setText(song.artist);
        holder.itemView.setTag(mItemList.get(position));
    }

    @Override
    public long getUniqueItemId(int position) {
        return mItemList.get(position).first;
    }

    public void updateState(State state) {
        this.playlist = state.selectedPlaylist;
        this.songPlaying = state.songPlaying;
        ArrayList<Pair<Long, Song>> list = new ArrayList<>();
        for (Song song : playlist.songs) {
            list.add(new Pair<>(song.id, song));
        }
        System.out.println(">> Set songs list");
        setItemList(list);
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        TextView songName;
        TextView songArtist;
        private Song song;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            songName = itemView.findViewById(R.id.songName);
            songArtist = itemView.findViewById(R.id.songArtist);
            ((ListSwipeItem) itemView).setSupportedSwipeDirection(ListSwipeItem.SwipeDirection.RIGHT);
        }

        void setSong(Song song) {
            this.song = song;
            songName.setText(song.name);
            songArtist.setText(song.artist);

            int color = songColor(song);
            songName.setTextColor(color);
            songArtist.setTextColor(color);
        }

        private int songColor(Song song) {
            if (song.isMissing)
                return Color.parseColor("#e53935"); // RED

            if (song == songPlaying)
                return Color.parseColor("#03a9f4"); // Blue

            return Color.WHITE;
        }

        @Override
        public void onItemClicked(View view) {
            dispatch(new Play(playlist, playlist.songs.indexOf(song)));
        }

        @Override
        public boolean onItemLongClicked(View view) {
            Toast.makeText(view.getContext(), "Item long clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}