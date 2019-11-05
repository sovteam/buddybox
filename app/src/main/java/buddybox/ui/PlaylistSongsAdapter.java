package buddybox.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.woxthebox.draglistview.DragItemAdapter;
import com.woxthebox.draglistview.swipe.ListSwipeItem;

import java.util.ArrayList;

import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.PlayPlaylist;
import buddybox.core.events.SongSelected;
import sov.buddybox.R;

import static buddybox.ui.ModelProxy.dispatch;

class PlaylistSongsAdapter extends DragItemAdapter<Pair<Long, Song>, PlaylistSongsAdapter.ViewHolder> {

    private final Context context;
    private final int mLayoutId;
    private final int mGrabHandleId;
    private final boolean mDragOnLongPress;
    private State lastState;

    PlaylistSongsAdapter(Context context, int layoutId, int grabHandleId, boolean dragOnLongPress) {
        this.context = context;
        mLayoutId = layoutId;
        mGrabHandleId = grabHandleId;
        mDragOnLongPress = dragOnLongPress;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Song song = mItemList.get(position).second;
        if (song == null)
            return;
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
        ArrayList<Pair<Long, Song>> list = new ArrayList<>();
        for (Song song : state.selectedPlaylist.songs) {
            list.add(new Pair<>(song.getId(), song));
        }
        setItemList(list);
        lastState = state;
    }

    class ViewHolder extends DragItemAdapter.ViewHolder {
        private final ImageView drag;
        private final TextView songName;
        private final TextView songArtist;
        private final ImageView albumArt;
        private Song song;

        ViewHolder(final View itemView) {
            super(itemView, mGrabHandleId, mDragOnLongPress);
            drag = itemView.findViewById(R.id.drag);
            albumArt = itemView.findViewById(R.id.albumArt);
            songName = itemView.findViewById(R.id.songName);
            songArtist = itemView.findViewById(R.id.songArtist);

            ((ListSwipeItem) itemView).setSupportedSwipeDirection(ListSwipeItem.SwipeDirection.RIGHT);
        }

        void setSong(Song song) {
            this.song = song;
            songName.setText(song.name);
            songArtist.setText(song.artist);

            int color = Color.WHITE;
            int icon = R.drawable.ic_drag_handle;
            if (song.isMissing) {
                color = Color.parseColor("#e53935"); // RED
                icon = R.drawable.ic_drag_handle_red;
            } else if (song == lastState.songPlaying && lastState.selectedPlaylist == lastState.playlistPlaying) {
                color = Color.parseColor("#03a9f4"); // BLUE
                icon = R.drawable.ic_drag_handle_blue;
            }
            songName.setTextColor(color);
            songArtist.setTextColor(color);
            drag.setImageResource(icon);

            Bitmap art = song.getArt();
            if (art != null)
                albumArt.setImageBitmap(art);
            else
                albumArt.setImageResource(R.mipmap.sneer2);
        }

        @Override
        public void onItemClicked(View view) {
            dispatch(new PlayPlaylist(
                    lastState.selectedPlaylist,
                    lastState.selectedPlaylist.indexOf(song, lastState.isShuffle)));
        }

        @Override
        public boolean onItemLongClicked(View view) {
            dispatch(new SongSelected(song.hash.toString()));
            context.startActivity(new Intent(context, EditSongActivity.class));
            return true;
        }
    }
}