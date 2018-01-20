package buddybox.core.events;

import buddybox.core.Dispatcher;

public class Sampler {
    public static final Dispatcher.Event SAMPLER_START = new Dispatcher.Event("SamplerStart");
    public static final Dispatcher.Event SAMPLER_STOP = new Dispatcher.Event("SamplerStop");
    public static final Dispatcher.Event LOVED_VIEWED = new Dispatcher.Event("LovedViewed");
}
