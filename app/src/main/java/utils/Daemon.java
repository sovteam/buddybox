package utils;

public class Daemon extends Thread {

    public Daemon(String name) {
        super(name);
        setDaemon(true);
        start();
    }

}
