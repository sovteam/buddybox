package buddybox.api;

public class Sampler {
    public static final Model.Event SAMPLER_START = new Model.Event("SamplerStart");
    public static final Model.Event SAMPLER_STOP = new Model.Event("SamplerStop");
    public static final Model.Event SAMPLER_LOVE = new Model.Event("SamplerLove");
    public static final Model.Event SAMPLER_HATE = new Model.Event("SamplerHate");
    public static final Model.Event SAMPLER_DELETE = new Model.Event("SamplerDelete");

    public static final Model.Event LOVED_VIEWED = new Model.Event("LovedViewed");
}
