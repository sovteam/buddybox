package buddybox.core;

import java.util.Arrays;

public class Hash {

    public final byte[] bytes;

    public Hash(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

}