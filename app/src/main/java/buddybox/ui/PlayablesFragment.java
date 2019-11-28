package buddybox.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import buddybox.ui.util.AsyncImage;
import sov.buddybox.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import buddybox.core.Album;
import buddybox.core.Artist;
import buddybox.core.Playable;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.Play;
import buddybox.ui.library.dialogs.SelectPlaylistDialogFragment;

import static buddybox.model.Model.ALL_SONGS;
import static buddybox.ui.ModelProxy.dispatch;

public class PlayablesFragment extends Fragment {

    private Activity activity;
    private PlayablesArrayAdapter playablesAdapter;
    private List<Playable> playables;
    private State lastState;
    private View view;
    private String textToHighlight;

    public PlayablesFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playables, container, false);
        this.activity = getActivity();

        ListView list = view.findViewById(R.id.playablesList);
        View footer = inflater.inflate(R.layout.list_footer, list, false);
        list.addFooterView(footer);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            startActivity(new Intent(getContext(), PlayingActivity.class));
            dispatch(new Play(playables.get(i)));
        }});
        playablesAdapter = new PlayablesArrayAdapter();
        list.setAdapter(playablesAdapter);

        return view;
    }

    public void update(List<Playable> playables, State state) {
        this.playables = playables;
        this.lastState = state;

        // show/hide footers
        if (state.syncLibraryPending) {
            view.findViewById(R.id.footerLoading).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.footerLoading).setVisibility(View.GONE);
        }

        playablesAdapter.update();
    }

    public void setTextToHighlight(String text) {
        textToHighlight = text;
    }

    class PlayablesArrayAdapter extends ArrayAdapter<Playable> {

        PlayablesArrayAdapter() {
            super(activity, -1, new ArrayList<Playable>());
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            Playable playable = getItem(position);
            View rowView = convertView == null
                    ? activity.getLayoutInflater().inflate(R.layout.playable_item, parent, false)
                    : convertView;

            if (playable == null)
                return rowView;

            TextView name = rowView.findViewById(R.id.name);
            TextView subtitle = rowView.findViewById(R.id.subtitle);
            name.setText(highlightText(playable.name()));
            subtitle.setText(highlightText(playable.subtitle()));

            // playlist playing or song playing
            if (playable == lastState.playlistPlaying || (lastState.songPlaying == playable && lastState.playlistPlaying.name().equals(ALL_SONGS))) {
                name.setTextColor(Color.parseColor("#4fc3f7"));
                subtitle.setTextColor(Color.parseColor("#4fc3f7"));
            } else {
                name.setTextColor(Color.WHITE);
                subtitle.setTextColor(Color.LTGRAY);
            }

            if (playable.getClass() == Song.class)
                updateSongItem((Song) playable, rowView);
            else
                updatePlayableItem(playable, rowView);

            return rowView;
        }

        private void updatePlayableItem(Playable playable, View rowView) {
            rowView.findViewById(R.id.addToPlaylist).setVisibility(View.GONE);
            if (playable.getClass() == Artist.class) {
                int icon = playable == lastState.playlistPlaying
                        ? R.drawable.ic_person_blue
                        : R.drawable.ic_person;
                ((ImageView)rowView.findViewById(R.id.playableArt)).setImageResource(icon);
            }
            if (playable.getClass() == Playlist.class) {
                int icon = playable == lastState.playlistPlaying
                        ? R.drawable.ic_queue_music_blue
                        : R.drawable.ic_queue_music;
                ((ImageView)rowView.findViewById(R.id.playableArt)).setImageResource(icon);
            }
            if (playable.getClass() == Album.class) {
                int icon = playable == lastState.playlistPlaying
                        ? R.drawable.ic_library_music_blue
                        : R.drawable.ic_library_music_grey;
                ((ImageView)rowView.findViewById(R.id.playableArt)).setImageResource(icon);
            }
        }

        private void updateSongItem(final Song song, View rowView) {
            ImageView addToPlaylist = rowView.findViewById(R.id.addToPlaylist);
            addToPlaylist.setVisibility(View.VISIBLE);
            addToPlaylist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openSelectPlaylistDialog(song);
                }
            });

            AsyncImage.setImage((ImageView)rowView.findViewById(R.id.playableArt), song);
        }

        private Spannable highlightText(String text) {
            Spannable spannable = new SpannableString(text);
            if (textToHighlight != null && !textToHighlight.isEmpty()) {
                int startPos = text.toLowerCase().indexOf(textToHighlight.toLowerCase(Locale.US));
                int endPos = startPos + textToHighlight.length();

                if (startPos != -1) {
                    ColorStateList color = new ColorStateList(new int[][]{new int[]{}}, new int[]{Color.parseColor("#66bb6a")});
                    TextAppearanceSpan highlightSpan = new TextAppearanceSpan(null, Typeface.BOLD, -1, color, null);
                    spannable.setSpan(highlightSpan, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    return spannable;
                }
            }
            return spannable;
        }

        void update() {
            clear();
            addAll(playables);
        }
    }

    private void openSelectPlaylistDialog(Song song) {
        SelectPlaylistDialogFragment frag = new SelectPlaylistDialogFragment();

        // Select playlists song is not associated
        List<Playlist> playlistsForSong = new ArrayList<>();
        for (Playlist playlist : lastState.playlists){
            if (!playlist.hasSong(song))
                playlistsForSong.add(playlist);
        }

        // Get playlists info from bundle
        long[] playlistIds = new long[playlistsForSong.size()];
        ArrayList<String> playlistNames = new ArrayList<>();
        for (int i = 0; i < playlistsForSong.size(); i++) {
            Playlist playlist = playlistsForSong.get(i);
            playlistIds[i] = playlist.getId();
            playlistNames.add(playlist.name);
        }

        Bundle args = new Bundle();
        args.putString("songHash", song.hash.toString());
        args.putStringArrayList("playlistsNames", playlistNames);
        args.putLongArray("playlistsIds", playlistIds);
        frag.setArguments(args);

        FragmentManager fragManager = getFragmentManager();
        if (fragManager != null)
            frag.show(fragManager, "Select Playlist");
    }
}
