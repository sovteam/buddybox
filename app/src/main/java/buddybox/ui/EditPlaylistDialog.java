package buddybox.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.adalbertosoares.buddybox.R;

import buddybox.core.events.CreatePlaylist;
import buddybox.core.events.SetPlaylistName;

import static buddybox.ui.ModelProxy.dispatch;

public class EditPlaylistDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_edit_playlist, null);
        final EditText edit = (EditText)view.findViewById(R.id.playlistName);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setText(getArguments().getString("playlistName"));

        builder.setView(view)
                .setTitle("Edit Playlist Name")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String playlistName = edit.getText().toString().trim();
                        if (playlistName.isEmpty()) {
                            Toast.makeText(getContext(), "Playlist name can\'t be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        dispatch(new SetPlaylistName(playlistName));
                        Toast.makeText(getContext(), "Playlist name saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditPlaylistDialog.this.getDialog().cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }
}
