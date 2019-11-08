package buddybox.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import buddybox.core.events.SongDeleteRequest;
import buddybox.core.events.SongSelected;

import static buddybox.ui.ModelProxy.dispatch;

public class SongOptionsDialog extends DialogFragment {

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
        final String songHash = args.getString("songHash");
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
        dispatch(new SongSelected(songHash));
        startActivity(new Intent(getContext(), EditSongActivity.class));
    }

    private void deleteSong(String songHash) {
        dispatch(new SongDeleteRequest(songHash));
        Toast.makeText(getContext(), "Song deleted", Toast.LENGTH_SHORT).show();
    }
}
