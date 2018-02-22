package buddybox.io;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class CallDetectService extends Service {
    private CallHelper callHelper;

    public static void init(Context context) {
        Intent intent = new Intent(context, CallDetectService.class);
        context.startService(intent);
    }

    public CallDetectService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        callHelper = new CallHelper(this);

        int res = super.onStartCommand(intent, flags, startId);
        callHelper.start();
        return res;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        callHelper.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
