package utils;

public class Utils {

    static public void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("Thread sleep interrupted: " + Thread.currentThread());
        }
    }

}
