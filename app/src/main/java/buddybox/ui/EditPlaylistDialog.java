package buddybox.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import sov.buddybox.R;

import buddybox.core.events.PlaylistSetName;

import static buddybox.ui.ModelProxy.dispatch;

public class EditPlaylistDialog extends DialogFragment {

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

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = View.inflate(getContext(), R.layout.dialog_edit_playlist, null);
        final EditText edit = view.findViewById(R.id.playlistName);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        Bundle args = getArguments();
        if (args != null)
            edit.setText(args.getString("playlistName"));

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
                        dispatch(new PlaylistSetName(playlistName));
                        Toast.makeText(getContext(), "Playlist name saved", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        EditPlaylistDialog.this.getDialog().cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }
}
