package circuitlord.reactivemusic;

import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.platform.BiomeTagHelper;
import circuitlord.reactivemusic.platform.PlatformHelper;
import org.rm_yaml.snakeyaml.Yaml;
import org.rm_yaml.snakeyaml.constructor.Constructor;
import rm_javazoom.jl.decoder.Bitstream;

import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class RMSongpackLoader {

    public static List<SongpackZip> availableSongpacks = new ArrayList<>();

    public static final String[] SONG_EXTENSIONS = { ".mp3", ".ogg", ".wav" };

    /** Zip filesystem reference with ownership (only close what you created, the player may share it). */
    private static class ZipFs {
        FileSystem fs;
        boolean owned;
    }

    private static ZipFs acquirePackZipFs(Path zipPath) throws IOException {
        ZipFs ref = new ZipFs();
        try {
            Map<String, String> env = new HashMap<>();
            env.put("create", "false");
            ref.fs = FileSystems.newFileSystem(zipPath, env);
            ref.owned = true;
        } catch (FileSystemAlreadyExistsException e) {
            ref.fs = FileSystems.getFileSystem(URI.create("jar:" + zipPath.toUri()));
            ref.owned = false;
        }
        return ref;
    }

    private static void releasePackZipFs(ZipFs ref) {
        if (ref != null && ref.owned && ref.fs != null) {
            try {
                ref.fs.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Resolves a song name to "music/&lt;name&gt;.&lt;ext&gt;". Bare names prefer .mp3
     * (legacy behavior), explicit .mp3/.ogg/.wav endings are honored. Null when nothing matches.
     */
    public static String resolveSongFile(Path dirPath, String song, boolean embedded) {
        String lower = song.toLowerCase(Locale.ROOT);
        for (String ext : SONG_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                String fileName = "music/" + song;
                return songFileExists(dirPath, fileName, embedded) ? fileName : null;
            }
        }
        for (String ext : SONG_EXTENSIONS) {
            String fileName = "music/" + song + ext;
            if (songFileExists(dirPath, fileName, embedded)) return fileName;
        }
        return null;
    }

    private static boolean songFileExists(Path dirPath, String fileName, boolean embedded) {
        if (embedded) {
            try (InputStream in = RMSongpackLoader.class.getResourceAsStream("/musicpack/" + fileName)) {
                return in != null;
            } catch (IOException e) {
                return false;
            }
        }

        if (dirPath == null) return false;

        // Check if path is a zip file
        if (Files.isRegularFile(dirPath) && dirPath.toString().endsWith(".zip")) {
            ZipFs ref = null;
            try {
                ref = acquirePackZipFs(dirPath);
                return Files.exists(ref.fs.getPath(fileName));
            } catch (Exception e) {
                return false;
            } finally {
                releasePackZipFs(ref);
            }
        }

        return Files.exists(dirPath.resolve(fileName));
    }

    public static MusicPackResource getInputStream(Path dirPath, String fileName, boolean embedded) {
        MusicPackResource resource = new MusicPackResource();

        // quick-path for embedded files
        if (embedded) {
            resource.inputStream = RMSongpackLoader.class.getResourceAsStream("/musicpack/" + fileName);
            return resource;
        }

        if (dirPath == null) {
            ReactiveMusic.LOGGER.error("dirpath was null");
            return null;
        }

        // Check if path is a zip file
        if (Files.isRegularFile(dirPath) && dirPath.toString().endsWith(".zip")) {
            ZipFs ref = null;
            try {
                ref = acquirePackZipFs(dirPath);
                Path filePath = ref.fs.getPath(fileName);
                if (Files.exists(filePath)) {
                    resource.inputStream = Files.newInputStream(filePath);
                    // ownership transfers to the resource, do not release here
                    resource.fileSystem = ref.fs;
                    resource.ownsFileSystem = ref.owned;
                    ref.owned = false;
                    return resource;
                }
            } catch (IOException e) {
                ReactiveMusic.LOGGER.error("Failed while loading file from zip " + e.getMessage());
                return null;
            } catch (RuntimeException e) {
                ReactiveMusic.LOGGER.error("Failed while loading file from zip " + e.getMessage());
                return null;
            } finally {
                releasePackZipFs(ref);
            }
            return null;
        }

        // handle normal directories
        else {
            Path filePath = dirPath.resolve(fileName);
            if (Files.exists(filePath)) {
                try {
                    resource.inputStream = Files.newInputStream(filePath);
                    return resource;
                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error(e.toString());
                }
            } else {
                ReactiveMusic.LOGGER.error("Couldn't find file! " + filePath);
            }
        }

        return null;
    }

    public static void fetchAvailableSongpacks() {
        long startTime = System.currentTimeMillis();
        availableSongpacks.clear();

        // user-defined biome tags from the config, usable as BIOMETAG= in any songpack
        if (ReactiveMusic.config != null && ReactiveMusic.config.customBiomeTags != null) {
            for (String rawTag : ReactiveMusic.config.customBiomeTags) {
                if (rawTag == null || rawTag.isBlank()) continue;
                String path = RMRuntimeEntry.normalizeBiomeTagPath(rawTag);
                var biomeTag = BiomeTagHelper.INSTANCE.createBiomeTagKey(path);
                if (biomeTag != null) {
                    SongPicker.registerBiomeTag(biomeTag);
                } else {
                    ReactiveMusic.LOGGER.warn("Custom biome tag '" + rawTag + "' is not available on this platform, ignoring.");
                }
            }
        }

        // Load embedded songpack
        availableSongpacks.add(loadSongpack(null, true, "ReactiveMusic.yaml"));

        // Load user songpacks
        Path resourcePacksPath = PlatformHelper.INSTANCE.getGameDir().resolve("resourcepacks");
        List<Path> potentialPacks = new ArrayList<>();

        try {
            Files.walkFileTree(resourcePacksPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
                    potentialPacks.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            ReactiveMusic.LOGGER.error("Failed while visiting potential packs " + e.getMessage());
        }

        for (Path packPath : potentialPacks) {
            List<String> yamlFileNames = new ArrayList<>();

            // zip files
            if (Files.isRegularFile(packPath) && packPath.toString().endsWith(".zip")) {
                ZipFs ref = null;
                try {
                    ref = acquirePackZipFs(packPath);

                    Path root = ref.fs.getPath("/");

                    yamlFileNames = getYamlFiles(Files.list(root).toList());

                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error("Failed reading zip: " + e);
                    continue;
                } catch (RuntimeException e) {
                    ReactiveMusic.LOGGER.error("Failed reading zip: " + e);
                    continue;
                } finally {
                    releasePackZipFs(ref);
                }
            }

            // normal directories
            else if (Files.isDirectory(packPath)) {
                try {

                    yamlFileNames = getYamlFiles(Files.list(packPath).toList());

                } catch (IOException e) {
                    ReactiveMusic.LOGGER.error("Failed reading directory: " + e);
                    continue;
                }
            }

            for (String yamlFile : yamlFileNames) {
                SongpackZip songpackZip = loadSongpack(packPath, false, yamlFile);
                if (songpackZip != null) {
                    availableSongpacks.add(songpackZip);
                }
            }
        }

        ReactiveMusic.LOGGER.info("Took " + (System.currentTimeMillis() - startTime) + "ms to parse available songpacks, found " + availableSongpacks.size() + "!");
    }

    public static List<String> getYamlFiles(List<Path> paths) {

        List<String> found = new ArrayList<>();

        for (Path path : paths) {

            if (Files.isRegularFile(path)) {
                String filename = path.getFileName().toString();

                if (filename.toLowerCase().endsWith(".yaml")) {
                    found.add(filename);
                }
            }
        }

        return found;
    }


    // New version of loadSongpack with YAML file name
    public static SongpackZip loadSongpack(Path songpackPath, boolean embedded, String yamlFileName) {
        SongpackZip songpackZip = new SongpackZip();
        songpackZip.path = songpackPath;
        songpackZip.embedded = embedded;

        var configResource = getInputStream(songpackPath, yamlFileName, embedded);
        if (configResource == null || configResource.inputStream == null) {
            return null;
        }

        Yaml yaml = new Yaml();

        try {
            songpackZip.config = yaml.loadAs(configResource.inputStream, SongpackConfig.class);
        } catch (Exception e) {
            songpackZip.config = new SongpackConfig();
            songpackZip.config.name = songpackPath != null ? songpackPath.getFileName().toString() : "Embedded";
            songpackZip.errorString = e.toString() + "\n\n";
            songpackZip.blockLoading = true;

            ReactiveMusic.LOGGER.error("Failed to load properties! Embedded=" + embedded + " Exception:" + e.toString());
        }

        // yaml.loadAs can return null for empty/invalid YAML without throwing
        if (songpackZip.config == null) {
            songpackZip.config = new SongpackConfig();
            songpackZip.config.name = songpackPath != null ? songpackPath.getFileName().toString() : "Embedded";
            songpackZip.errorString += "YAML config was empty or invalid.\n\n";
            songpackZip.blockLoading = true;

            ReactiveMusic.LOGGER.error("YAML config was null after parsing! Embedded=" + embedded);
        }

        if (!Constructor.errorString.isEmpty()) {
            songpackZip.errorString += Constructor.errorString;
        }

        if (Constructor.blockLoading) {
            songpackZip.blockLoading = true;
        }

        verifySongpackZip(songpackZip);

        if (!songpackZip.blockLoading) {
            songpackZip.runtimeEntries = getRuntimeEntries(songpackZip);
        }

        return songpackZip;
    }

    // Legacy call for default "ReactiveMusic.yaml"
    public static SongpackZip loadSongpack(Path songpackPath, boolean embedded) {
        return loadSongpack(songpackPath, embedded, "ReactiveMusic.yaml");
    }

    private static List<RMRuntimeEntry> getRuntimeEntries(SongpackZip songpackZip) {
        List<RMRuntimeEntry> runtimeEntries = new ArrayList<>();

        for (var entry : songpackZip.config.entries) {
            if (entry == null) continue;

            RMRuntimeEntry runtimeEntry = RMRuntimeEntry.create(songpackZip, entry);

            if (!runtimeEntry.errorString.isEmpty()) {
                songpackZip.errorString += runtimeEntry.errorString;

                // bail out, we don't want to potentially have a half valid event that takes more priority than it should
                continue;
            }

            if (runtimeEntry.conditions.isEmpty()) continue;

            // entries with no playable songs would win priority and play silence forever
            if (runtimeEntry.songs.isEmpty()) {
                songpackZip.errorString += "Entry \"" + runtimeEntry.eventString + "\" has no valid songs left and will be skipped.\n\n";
                continue;
            }

            runtimeEntries.add(runtimeEntry);
        }

        return runtimeEntries;
    }

    public static void verifySongpackZip(SongpackZip songpackZip) {
        if (songpackZip.config == null || songpackZip.config.entries == null) {
            songpackZip.errorString += "Entries are null or not formatted correctly! Make sure you.\n\n";
            songpackZip.blockLoading = true;
            return;
        }

        for (int i = 0; i < songpackZip.config.entries.length; i++) {
            if (songpackZip.config.entries[i] == null) continue;

            if (songpackZip.config.entries[i].alwaysPlay || songpackZip.config.entries[i].alwaysStop) {
                songpackZip.errorString += "WARNING! You are using a songpack made for Reactive Music v0.5 and older, things may not work well!\n\n";
                songpackZip.convertBiomeToBiomeTag = true;
                songpackZip.isv05OldSongpack = true;
                break;
            }
        }

        // Check if all the songs are valid
        for (int i = 0; i < songpackZip.config.entries.length; i++) {
            if (songpackZip.config.entries[i] == null || songpackZip.config.entries[i].songs == null) continue;

            // loop over songs backwards so we can remove invalid entries
            for (int j = songpackZip.config.entries[i].songs.length - 1; j >= 0; j--) {

                String song = songpackZip.config.entries[i].songs[j];
                String fileName = resolveSongFile(songpackZip.path, song, songpackZip.embedded);

                if (fileName == null) {
                    StringBuilder eventName = new StringBuilder();
                    for (int k = 0; k < songpackZip.config.entries[i].events.length; k++) {
                        eventName.append(songpackZip.config.entries[i].events[k].toString());
                    }

                    songpackZip.errorString += "Failed finding song: \"" + song + "\" for event: \"" + eventName + "\" (looked for .mp3/.ogg/.wav in " + describeSongLocation(songpackZip) + ")\n\n";

                    // remove this invalid audio file
                    // this is unoptimized and slow but it's fine
                    java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(songpackZip.config.entries[i].songs));
                    list.remove(j);
                    songpackZip.config.entries[i].songs = list.toArray(new String[0]);



                    // songpackZip.blockLoading = true;
                    continue;
                }

                var inputStream = getInputStream(songpackZip.path, fileName, songpackZip.embedded);

                if (inputStream == null || inputStream.inputStream == null) {
                    StringBuilder eventName = new StringBuilder();
                    for (int k = 0; k < songpackZip.config.entries[i].events.length; k++) {
                        eventName.append(songpackZip.config.entries[i].events[k].toString());
                    }

                    songpackZip.errorString += "Failed opening song: \"" + song + "\" for event: \"" + eventName + "\"\n\n";

                    java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(songpackZip.config.entries[i].songs));
                    list.remove(j);
                    songpackZip.config.entries[i].songs = list.toArray(new String[0]);
                } else if (!isPlayableAudio(inputStream.inputStream, fileName)) {
                    StringBuilder eventName = new StringBuilder();
                    for (int k = 0; k < songpackZip.config.entries[i].events.length; k++) {
                        eventName.append(songpackZip.config.entries[i].events[k].toString());
                    }

                    String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
                    songpackZip.errorString += "Song \"" + song + "\" for event: \"" + eventName + "\" is not a valid " + ext + " file and will be skipped\n\n";

                    java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList(songpackZip.config.entries[i].songs));
                    list.remove(j);
                    songpackZip.config.entries[i].songs = list.toArray(new String[0]);

                    try {
                        inputStream.close();
                    } catch (Exception ignored) {}
                } else {
                    try {
                        inputStream.close();
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private static String describeSongLocation(SongpackZip songpackZip) {
        if (songpackZip.embedded) return "embedded musicpack";
        if (songpackZip.path != null) return songpackZip.path + "/music/";
        return "unknown path";
    }

    /**
     * Peeks at the stream with the real MP3 parser. Catches renamed containers
     * (m4a/mp4, ogg, wav) that would otherwise "play" as silence.
     */
    private static boolean isPlayableMp3(InputStream in) {
        try {
            Bitstream bitstream = new Bitstream(in);
            try {
                return bitstream.readFrame() != null;
            } finally {
                try {
                    bitstream.close();
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPlayableAudio(InputStream in, String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".ogg")) return isPlayableOgg(in);
            if (lower.endsWith(".wav")) return isPlayableWav(in);
            return isPlayableMp3(in);
        } catch (Exception e) {
            return false;
        }
    }

    /** OGG container magic, full decode check happens at playback (thread survives errors). */
    private static boolean isPlayableOgg(InputStream in) throws IOException {
        byte[] magic = new byte[4];
        int read = 0;
        while (read < 4) {
            int n = in.read(magic, read, 4 - read);
            if (n < 0) return false;
            read += n;
        }
        return magic[0] == 'O' && magic[1] == 'g' && magic[2] == 'g' && magic[3] == 'S';
    }

    /** WAV header parsed by the JDK itself, no audio hardware needed. */
    private static boolean isPlayableWav(InputStream in) {
        try (var audio = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in))) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
