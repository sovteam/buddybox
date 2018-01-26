package buddybox.ui.library;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import buddybox.core.events.AddSongToPlaylist;

import static buddybox.ui.ModelProxy.dispatch;

public class SelectPlaylistDialogFragment extends DialogFragment {
    private ArrayList<String> list;
    private String songId;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // TODO sort playlists by last modified
        list = getArguments().getStringArrayList("playlists");
        songId = getArguments().getString("playlists");
        Collections.sort(list);
        list.add(0, "<Create New Playlist>");

        builder.setTitle("Add Song to Playlist")
                .setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openNewPlaylistDialog(songId);
                        } else {
                            dispatch(new AddSongToPlaylist(list.get(which), getArguments().getString("songId")));
                        }
                        System.out.println(">>> Playlist selected " + which);
                        Toast.makeText(getContext(), "Song added to playlist", Toast.LENGTH_SHORT).show();
                    }
                });
        return builder.create();
    }

    private void openNewPlaylistDialog(String songId) {
        CreatePlaylistDialogFragment frag = new CreatePlaylistDialogFragment();

        Bundle args = new Bundle();
        args.putString("songId", songId); // TODO switch to songId
        frag.setArguments(args);

        frag.show(getFragmentManager(), "Create Playlist");
    }
}
