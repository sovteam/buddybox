package buddybox;

import android.os.FileObserver;

public class MyFileObserver extends FileObserver {
    
    public String absolutePath;
    
    public MyFileObserver(String path) {
        super(path, FileObserver.ALL_EVENTS);
        absolutePath = path;
    }
    
    @Override
    public void onEvent(int event, String path) {
        if (path == null) {
            return;
        }

        //a new file or subdirectory was created under the monitored directory
        if ((FileObserver.CREATE & event)!=0) {
            System.out.println(">>> FileObserver: " + absolutePath + "/" + path + " is created");
        }

        //a file or directory was opened
        if ((FileObserver.OPEN & event)!=0) {
            System.out.println(">>> FileObserver: " + path + " is opened");
        }

        //data was read from a file
        if ((FileObserver.ACCESS & event)!=0) {
            System.out.println(">>> FileObserver: " + absolutePath + "/" + path + " is accessed/read");
        }

        //data was written to a file
        if ((FileObserver.MODIFY & event)!=0) {
            System.out.println(">>> FileObserver: " + absolutePath + "/" + path + " is modified");
        }

        //someone has a file or directory open read-only, and closed it
        if ((FileObserver.CLOSE_NOWRITE & event)!=0) {
            System.out.println(">>> FileObserver: " + path + " is closed");
        }

        //someone has a file or directory open for writing, and closed it 
        if ((FileObserver.CLOSE_WRITE & event)!=0) {
            // TODO add new song
            System.out.println(">>> FileObserver: " + absolutePath + "/" + path + " is written and closed");
        }

        //[todo: consider combine this one with one below]
        //a file was deleted from the monitored directory
        if ((FileObserver.DELETE & event)!=0) {
            //for testing copy file
// FileUtils.copyFile(absolutePath + "/" + path);
            System.out.println(">>> FileObserver: " + absolutePath + "/" + path + " is deleted");
        }

        //the monitored file or directory was deleted, monitoring effectively stops
        if ((FileObserver.DELETE_SELF & event)!=0) {
            // TODO remove from songs
            System.out.println(">>> FileObserver: " + absolutePath + "/" + " is deleted");
        }

        //a file or subdirectory was moved from the monitored directory
        if ((FileObserver.MOVED_FROM & event)!=0) {
            System.out.println(">>> FileObserver: " + absolutePath + "/" + path + " is moved to somewhere ");
        }

        //a file or subdirectory was moved to the monitored directory
        if ((FileObserver.MOVED_TO & event)!=0) {
            System.out.println(">>> FileObserver: " + "File is moved to " + absolutePath + "/" + path);
        }

        //the monitored file or directory was moved; monitoring continues
        if ((FileObserver.MOVE_SELF & event)!=0) {
            System.out.println(">>> FileObserver: " + path + " is moved");
        }

        //Metadata (permissions, owner, timestamp) was changed explicitly
        if ((FileObserver.ATTRIB & event)!=0) {
            System.out.println(">>> FileObserver: " + absolutePath + "/" + path + " is changed (permissions, owner, timestamp)");
        }
    }
}