package buddybox.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import buddybox.core.events.DeletePlaylist;
import buddybox.core.events.RemoveSongFromPlaylist;

import static buddybox.ui.ModelProxy.dispatch;

public class PlaylistSongOptionsDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final Long playlistId = getArguments().getLong("playlistId");
        final String songHash = getArguments().getString("songHash");
        String[] options = new String[]{"Remove Song"};

        builder.setTitle("Song Options")
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            dispatch(new RemoveSongFromPlaylist(songHash, playlistId));
                            Toast.makeText(getContext(), "Song removed from playlist", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        return builder.create();
    }
}
