package buddybox.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import buddybox.core.events.DeletePlaylist;

import static buddybox.ui.ModelProxy.dispatch;

public class PlaylistOptionsDialog extends DialogFragment {

    private FragmentActivity activity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        Bundle args = getArguments();
        assert args != null;
        final Long playlistId = args.getLong("playlistId");
        final String playlistName = getArguments().getString("playlistName");
        String[] options = new String[]{"Edit Name", "Delete Playlist"};

        builder.setTitle("Playlist Options")
                .setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: editPlaylist(playlistName);
                                    break;
                            case 1: deletePlaylist(playlistId);
                                    break;
                            default: break;
                        }
                    }
                });
        return builder.create();
    }

    private void editPlaylist(String playlistName) {
        EditPlaylistDialog dialog = new EditPlaylistDialog();
        Bundle args = new Bundle();
        args.putString("playlistName", playlistName);
        dialog.setArguments(args);
        FragmentManager fragMan = getFragmentManager();
        assert fragMan != null;
        dialog.show(fragMan, "Edit Playlist Name");
    }

    private void deletePlaylist(Long playlistId) {
        dispatch(new DeletePlaylist(playlistId));
        Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
    }
}
