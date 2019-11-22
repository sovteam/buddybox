package buddybox.ui.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DeleteConfirmation {
    private Context context;

    public DeleteConfirmation(Context context) {
        this.context = context;
    }

    public void onSuccess(DialogInterface.OnClickListener songDeleted) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Deleting Song")
                .setMessage("Really want to delete?")
                .setPositiveButton("Yes, delete", songDeleted)
                .setNegativeButton("Cancel", null)
                .show();
    }
}
