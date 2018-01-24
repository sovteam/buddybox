package buddybox.controller;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;
import buddybox.core.Hash;
import buddybox.core.events.LibraryUpdated;
import buddybox.core.events.Permission;
import buddybox.core.Song;
import buddybox.core.events.SongAdded;

import static buddybox.core.Dispatcher.addListener;
import static buddybox.core.Dispatcher.dispatch;

public class Library {

    private static int nextId;
    private static Map<Hash, Song> songByHash;

    public static void init() {
        addListener(new Dispatcher.Listener() { @Override public void onEvent(Dispatcher.Event event) {
            handle(event);
        }});
    }

    private static void handle(Dispatcher.Event event) {
        if (event.getClass() == Permission.class)
            updatePermission((Permission) event);

        if (event.getClass() == SongAdded.class)
            songAdded((SongAdded) event);
    }

    private static void songAdded(SongAdded event) {
        System.out.println("@@@ Library received Song added: " + event.song.name);
        songByHash.put(event.song.hash, event.song);
        dispatch(new LibraryUpdated(allSongs(), artists()));
    }

    private static void updatePermission(Permission event) {
        if (event.code == Permission.WRITE_EXTERNAL_STORAGE) {
            if (event.granted && songByHash == null)
                synchronizeLibrary();
        }
    }

    private static void synchronizeLibrary() {
        songByHash = new HashMap<>();
        List<File> mp3Files = SongUtils.listLibraryMp3Files();
        Map<Hash, File> mp3Hashes = mp3Hashes(mp3Files);
        createNewSongs(mp3Hashes);
        markMissingSongs(mp3Hashes);

        dispatch(new LibraryUpdated(allSongs(), artists()));
    }

    private static List<Song> allSongs() {
        return new ArrayList<>(songByHash.values());
    }

    private static ArrayList<Artist> artists() {
        Map<String, Artist> artistsMap = new HashMap<>();
        for (Song song : songByHash.values()) {
            Artist artist = artistsMap.get(song.artist);
            if (artist == null) {
                artist = new Artist(song.artist);
                artistsMap.put(song.artist, artist);
            }
            artist.addSong(song);
        }
        return new ArrayList<>(artistsMap.values());
    }

    private static Map<Hash, File> mp3Hashes(List<File> mp3Files) {
        Map<Hash, File> ret = new HashMap<>();
        for (File mp3 : mp3Files) {
            ret.put(mp3Hash(mp3), mp3);
        }
        return ret;
    }

    private static Hash mp3Hash(File mp3) {
        MessageDigest sha256 = getMessageDigest();
        if (sha256 == null)
            throw new RuntimeException("Missing SHA-256 algorithm");

        Hash ret = null;
        byte[] raw = SongUtils.rawMP3(mp3);
        if (raw != null) {
            byte[] hashBytes = Arrays.copyOf(sha256.digest(raw), 16); // 128 bits is enough
            ret = new Hash(hashBytes);
        }
        return ret;
    }

    private static MessageDigest getMessageDigest() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digest;
    }

    private static void createNewSongs(Map<Hash, File> files) {
        for (Hash hash : files.keySet()) {
            if (hasMp3(hash))
                updateSongPath(hash, files.get(hash).getPath());
            else
                createNewSong(hash, files.get(hash));
        }
    }

    private static boolean hasMp3(Hash hash) {
        /** TODO
         * return Song.exists(hash);
         * */
        return songByHash.containsKey(hash);
    }

    private static void updateSongPath(Hash hash, String path) {
        /** TODO
         * Song.updatePath(hash, path);
         */
    }

    private static void createNewSong(Hash hash, File file) {
        Map<String, String> metadata = SongUtils.readMp3Metadata(file);
        /** TODO
         * Song.create(hash, file.getPath, metadata);
         */

        Integer duration = null;
        String durationStr = metadata.get("duration");
        if (durationStr != null)
            duration = Integer.parseInt(durationStr);

        Song song = new Song(nextId(), hash, metadata.get("name"), metadata.get("artist"), metadata.get("genre"), duration, file.getPath(), file);
        songByHash.put(hash, song);
    }

    private static int nextId() {
        return nextId++;
    }

    private static void markMissingSongs(Map<Hash, File> files) {
        /** TODO
         * Song.setFileMissing(hash);
         * Song.setFileNotMissing(hash);
         */
        for (Hash hash : songByHash.keySet()) {
            if (!files.containsKey(hash))
                songByHash.get(hash).setMissing();
            else
                songByHash.get(hash).setNotMissing();
        }
    }

}
