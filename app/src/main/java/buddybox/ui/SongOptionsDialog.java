package buddybox.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import buddybox.core.events.DeletePlaylist;
import buddybox.core.events.SongDeleteRequest;
import buddybox.core.events.SongDeleted;

import static buddybox.ui.ModelProxy.dispatch;

public class SongOptionsDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final String songHash = getArguments().getString("songHash");
        String[] options = new String[]{"Edit Song", "Delete Song"};

        builder.setTitle("Song Options")
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: editSong(songHash);
                                    break;
                            case 1: deleteSong(songHash);
                                    break;
                            default: break;
                        }
                    }
                });
        return builder.create();
    }

    private void editSong(String songHash) {
        System.out.println(">>> Go fish!");
    }

    private void deleteSong(String songHash) {
        dispatch(new SongDeleteRequest(songHash));
        Toast.makeText(getContext(), "Song deleted", Toast.LENGTH_SHORT).show(); // TODO model should toast?
    }
}
