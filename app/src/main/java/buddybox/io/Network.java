package buddybox.io;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class Network {

    // TODO create service to update Network connection state and dispatch network state to model

    static boolean hasConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;

        NetworkInfo info = cm.getActiveNetworkInfo();
        return info.isConnected();

        /*  TODO check internet type according to user preferences
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
        */
    }
}
