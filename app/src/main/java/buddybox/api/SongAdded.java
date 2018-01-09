package buddybox.api;

public class SongAdded extends Core.Event {
    public final String filePath;

    public SongAdded(String filePath) {
        super("SongAdded " + filePath);
        this.filePath = filePath;
    }

}
