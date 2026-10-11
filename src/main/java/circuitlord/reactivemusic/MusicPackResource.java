package circuitlord.reactivemusic;

import java.io.InputStream;
import java.nio.file.FileSystem;

public class MusicPackResource {

    public FileSystem fileSystem = null;

    public InputStream inputStream = null;

    // false when the filesystem is shared (another song playing from the same zip),
    // only close what you opened or you cut the other reader off
    public boolean ownsFileSystem = true;

    public void close() throws Exception {
        try {
            if (inputStream != null) inputStream.close();
        } finally {
            if (ownsFileSystem && fileSystem != null) fileSystem.close();
        }
    }


}
