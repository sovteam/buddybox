package buddybox.ui.library.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import sov.buddybox.R;

import buddybox.core.events.PlaylistCreate;

import static buddybox.ui.ModelProxy.dispatch;

public class CreatePlaylistDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        final EditText edit = view.findViewById(R.id.playlistName);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        builder.setView(view)
                .setTitle("New Playlist")
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String playlistName = edit.getText().toString().trim();
                        if (playlistName.isEmpty()) {
                            Toast.makeText(getContext(), "Playlist name can\'t be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Bundle args = getArguments();
                        if (args != null) {
                            dispatch(new PlaylistCreate(playlistName, args.getString("songHash")));
                            Toast.makeText(getContext(), "Playlist created with song", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CreatePlaylistDialogFragment.this.getDialog().cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }
}
