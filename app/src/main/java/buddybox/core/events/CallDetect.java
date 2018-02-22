package buddybox.core.events;

import buddybox.core.Dispatcher;

public class CallDetect {
    public static final Dispatcher.Event PHONE_IDLE = new Dispatcher.Event("PhoneIdle");
    public static final Dispatcher.Event OUTGOING_CALL = new Dispatcher.Event("OutgoingCall");
    public static final Dispatcher.Event RECEIVING_CALL = new Dispatcher.Event("ReceivingCall");
}