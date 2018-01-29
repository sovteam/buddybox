package buddybox.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import buddybox.core.events.DeletePlaylist;

import static buddybox.ui.ModelProxy.dispatch;

public class PlaylistOptionsDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final Long playlistId = getArguments().getLong("playlistId");
        String[] options = new String[]{"Edit Name", "Delete Playlist"};

        builder.setTitle("Playlist Options")
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 1) {
                            dispatch(new DeletePlaylist(playlistId));
                            Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        return builder.create();
    }
}
