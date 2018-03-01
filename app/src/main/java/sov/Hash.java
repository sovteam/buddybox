package sov;

import android.util.Base64;
import java.util.Arrays;

public class Hash {

    public final byte[] bytes;

    public Hash(byte[] bytes) {
        this.bytes = bytes;
    }

    public Hash(String base64) {
        this.bytes = Base64.decode(base64, Base64.NO_PADDING);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return Base64.encodeToString(bytes, Base64.NO_PADDING);
    }

    @Override
    public boolean equals(Object obj) {
        return  obj != null &&
                obj.getClass() == Hash.class &&
                Arrays.equals(bytes, ((Hash)obj).bytes);
    }
}