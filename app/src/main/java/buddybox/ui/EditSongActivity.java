package buddybox.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.adalbertosoares.buddybox.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.SongUpdate;
import buddybox.io.SongUtils;

import static buddybox.ui.ModelProxy.dispatch;

public class EditSongActivity extends AppCompatActivity {

    private IModel.StateListener listener;
    private Song song;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_song);

        // Set events
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) {
            cancel();
        }});
        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() { @Override public void onClick(View view) { save(); }});

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
        if (song == null) {
            song = state.selectedSong;

            // Set fields
            EditText name = (EditText)findViewById(R.id.songName);
            name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            name.setText(song.name);

            EditText artist = (EditText)findViewById(R.id.songArtist);
            artist.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            artist.setText(song.artist);

            EditText album = (EditText)findViewById(R.id.songAlbum);
            album.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            album.setText(song.album);

            // List genres
            Spinner spinner = (Spinner) findViewById(R.id.songGenre);
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
            TextView text1 = (TextView) rowView.findViewById(android.R.id.text1);
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

        TextView nameError = (TextView)findViewById(R.id.songNameError);
        if (values.get("name").isEmpty()) {
            nameError.setText("Song name cannot be empty");
            return;
        }
        nameError.setText("");

        dispatch(new SongUpdate(song, values.get("name"), values.get("artist"), values.get("album"), values.get("genre")));
        finish();
        Toast.makeText(this, "Song changes saved", Toast.LENGTH_SHORT).show();
    }
}