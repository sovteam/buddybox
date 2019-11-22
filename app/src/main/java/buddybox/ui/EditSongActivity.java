package buddybox.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import buddybox.ui.helpers.DeleteConfirmation;
import sov.buddybox.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.PlaylistRemoveSong;
import buddybox.core.events.SongDeleteRequest;
import buddybox.core.events.SongUpdate;
import buddybox.io.SongUtils;
import buddybox.ui.util.FlowLayout;

import static buddybox.ui.ModelProxy.dispatch;

public class EditSongActivity extends AppCompatActivity {

    private IModel.StateListener listener;
    private Song song;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_song);

        // Set events
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            cancel();
        }});
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { save(); }});
        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { delete(); }});

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

    private void delete() {
        new DeleteConfirmation(this).onSuccess(new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {
            dispatch(new SongDeleteRequest(song.hash.toString()));
            Toast.makeText(EditSongActivity.this, "Song deleted", Toast.LENGTH_SHORT).show();
            finish();
        }});
    }

    private void updateState(State state) {
        song = state.selectedSong;

        // Set fields
        EditText name = findViewById(R.id.songName);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setText(song.name);

        EditText artist = findViewById(R.id.songArtist);
        artist.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        artist.setText(song.artist);

        EditText album = findViewById(R.id.songAlbum);
        album.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        album.setText(song.album);

        // List genres
        Spinner spinner = findViewById(R.id.songGenre);
        List<String> genres = new ArrayList<>(SongUtils.genreMap().values());
        List<String> allGenres = new ArrayList<>();
        allGenres.add("Select song genre");
        if (!genres.contains(song.genre))
            allGenres.add(song.genre);
        if (!song.genre.equals("Unknown Genre"))
            allGenres.add("Unknown Genre");

        Collections.sort(genres, new Comparator<String>() { @Override public int compare(String s1, String s2) {return s1.compareTo(s2);
        }});
        allGenres.addAll(genres);

        // Set spinner adapter
        GenresArrayAdapter adapter = new GenresArrayAdapter();
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(allGenres);
        spinner.setAdapter(adapter);
        spinner.setSelection(allGenres.indexOf(song.genre));

        ((TextView)findViewById(R.id.fileLength)).setText(String.format("File size: %s", song.printFileLength()));
        ((TextView)findViewById(R.id.songDuration)).setText(String.format("Duration: %s", song.duration()));

        // Show playlists that includes song
        List<Playlist> playlists = state.playlistsBySong.get(song.hash.toString());
        if (playlists == null || playlists.isEmpty()) {
            findViewById(R.id.playlists).setVisibility(View.GONE);
        } else {
            findViewById(R.id.playlists).setVisibility(View.VISIBLE);
            FlowLayout container = findViewById(R.id.playlistsChips);
            container.removeAllViews(); // TODO optimize
            for (final Playlist playlist : playlists) {
                final View chip = getLayoutInflater().inflate(R.layout.chip, null);
                ((TextView) chip.findViewById(R.id.chipText)).setText(playlist.name());
                chip.findViewById(R.id.removeIcon).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                    dispatch(new PlaylistRemoveSong(playlist, song));
                    Toast.makeText(getApplicationContext(), "Song removed from playlist", Toast.LENGTH_SHORT).show();
                }});
                container.addView(chip);
            }
        }

        if (song.isMissing) {
            findViewById(R.id.delete).setVisibility(View.GONE);
            findViewById(R.id.file_missing_text).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.delete).setVisibility(View.VISIBLE);
            findViewById(R.id.file_missing_text).setVisibility(View.GONE);
        }
    }

    private class GenresArrayAdapter extends ArrayAdapter<String> {
        GenresArrayAdapter() {
            super(EditSongActivity.this, -1, new ArrayList<String>());
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView == null
                    ? getLayoutInflater().inflate(android.R.layout.simple_spinner_item, parent, false)
                    : convertView;

            String genre = getItem(position);
            TextView text1 = rowView.findViewById(android.R.id.text1);
            text1.setText(genre);

            return rowView;
        }
    }

    private void cancel() {
        finish();
        if (hasChanges())
            Toast.makeText(this, "Song changes discarded", Toast.LENGTH_SHORT).show();
    }

    private Map<String, String> getValues() {
        Map<String,String> ret = new HashMap<>();
        ret.put("name", ((EditText)findViewById(R.id.songName)).getText().toString().trim());
        ret.put("artist", ((EditText)findViewById(R.id.songArtist)).getText().toString().trim());
        ret.put("album", ((EditText)findViewById(R.id.songAlbum)).getText().toString().trim());
        ret.put("genre", ((Spinner)findViewById(R.id.songGenre)).getSelectedItem().toString());
        return ret;
    }

    private boolean hasChanges() {
        Map<String,String> values = getValues();
        return  !Objects.equals(song.name, values.get("name")) ||
                !Objects.equals(song.artist, values.get("artist")) ||
                !Objects.equals(song.album, values.get("album")) ||
                !Objects.equals(song.genre, values.get("genre"));
    }

    private void save() {
        Map<String,String> values = getValues();

        TextView nameError = findViewById(R.id.songNameError);
        if (values.get("name").isEmpty()) {
            nameError.setText(R.string.SongNameCantBeEmpty);
            return;
        }
        nameError.setText("");

        dispatch(new SongUpdate(song, values.get("name"), values.get("artist"), values.get("album"), values.get("genre")));
        finish();
        Toast.makeText(this, "Song changes saved", Toast.LENGTH_SHORT).show();
    }
}
