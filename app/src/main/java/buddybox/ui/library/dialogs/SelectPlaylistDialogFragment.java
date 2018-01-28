package buddybox.ui.library.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import buddybox.core.events.AddSongToPlaylist;

import static buddybox.ui.ModelProxy.dispatch;

public class SelectPlaylistDialogFragment extends DialogFragment {
    private String songHash;
    private long[] playlistsIds;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        List<String> playlistsNames = getArguments().getStringArrayList("playlistsNames");
        if (playlistsNames == null) playlistsNames = new ArrayList<>();
        playlistsNames.add(0, "<Create New Playlist>");

        playlistsIds = getArguments().getLongArray("playlistsIds");
        songHash = getArguments().getString("songHash");

        System.out.println(">>> playlist songHash: " + songHash);

        builder.setTitle("Add Song to Playlist")
                .setItems(playlistsNames.toArray(new String[playlistsNames.size()]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            openNewPlaylistDialog(songHash);
                        } else {
                            dispatch(new AddSongToPlaylist(getArguments().getString("songHash"), playlistsIds[which - 1]));
                            Toast.makeText(getContext(), "Song added to playlist", Toast.LENGTH_SHORT).show(); // TODO do it thourgh updateState?
                        }
                        System.out.println(">>> Playlist selected " + which);
                    }
                });
        return builder.create();
    }

    private void openNewPlaylistDialog(String songHash) {
        CreatePlaylistDialogFragment frag = new CreatePlaylistDialogFragment();

        Bundle args = new Bundle();
        args.putString("songHash", songHash);
        frag.setArguments(args);

        frag.show(getFragmentManager(), "Create Playlist");
    }
}
