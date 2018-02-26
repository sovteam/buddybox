package buddybox.core.events;

import buddybox.core.Dispatcher;

public class AudioFocus {
    public static final Dispatcher.Event AUDIO_FOCUS_LOSS = new Dispatcher.Event("AudioFocusLoss");
    public static final Dispatcher.Event AUDIO_FOCUS_GAIN = new Dispatcher.Event("AudioFocusGain");
    public static final Dispatcher.Event AUDIO_FOCUS_LOSS_TRANSIENT = new Dispatcher.Event("AudioFocusLossTransient");
}
