package buddybox.api;

public class SelectFrame extends Core.Event {
    public final VisibleState.MainFrames frame;

    public SelectFrame(VisibleState.MainFrames frame) {
        super("SelectFrame");
        this.frame = frame;
    }

    public static final Core.Event SELECT_LIBRARY = new Core.Event("SelectLibrary");
    public static final Core.Event SELECT_SAMPLER = new Core.Event("SelectSampler");
}
