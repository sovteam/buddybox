package buddybox.io;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import static buddybox.core.Dispatcher.dispatch;

import static buddybox.core.events.CallDetect.OUTGOING_CALL;
import static buddybox.core.events.CallDetect.PHONE_IDLE;
import static buddybox.core.events.CallDetect.RECEIVING_CALL;

class CallHelper {
    private Context context;
    private CallStateListener callStateListener;
    private TelephonyManager telMan;

    CallHelper(Context context) {
        this.context = context;
        callStateListener = new CallStateListener();
    }

    void start() {
        telMan = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telMan == null)
            return;

        telMan.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    void stop() {
        telMan.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    dispatch(RECEIVING_CALL);
                    System.out.println(">>>> Ringing detected!!!");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    dispatch(OUTGOING_CALL);
                    System.out.println(">>>> Calling detected!!!");
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    dispatch(PHONE_IDLE);
                    System.out.println(">>>> Phone idle!!!");
                    break;
                default:
                    break;
            }
        }
    }
}
