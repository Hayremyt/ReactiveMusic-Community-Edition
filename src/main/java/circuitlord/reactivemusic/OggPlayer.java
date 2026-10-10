package circuitlord.reactivemusic;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * OGG Vorbis playback decoded with Minecraft's own LWJGL stb_vorbis (no new dependencies),
 * rendered on a shared PcmPlayer. No Minecraft classes besides LWJGL, safe on any version/loader.
 */
public class OggPlayer {

    public volatile boolean queuedToStop = false;

    private volatile boolean complete = false;
    private long handle = 0;
    private ByteBuffer fileData;
    private PcmPlayer pcm;
    private int channels;
    private short[] scratch = new short[0];

    public boolean isDone() {
        return complete;
    }

    public OggPlayer(InputStream in) throws Exception {
        byte[] bytes = in.readAllBytes();
        fileData = MemoryUtil.memAlloc(bytes.length);
        fileData.put(bytes);
        fileData.flip();

        IntBuffer error = MemoryUtil.memAllocInt(1);
        try {
            handle = STBVorbis.stb_vorbis_open_memory(fileData, error, null);
            if (handle == 0) {
                throw new Exception("not a valid OGG Vorbis stream (stb error " + error.get(0) + ")");
            }

            STBVorbisInfo info = STBVorbisInfo.malloc();
            try {
                STBVorbis.stb_vorbis_get_info(handle, info);
                channels = info.channels();
                if (channels < 1 || channels > 8) {
                    throw new Exception("unsupported channel count: " + channels);
                }
                pcm = new PcmPlayer(info.sample_rate(), channels);
            } finally {
                info.free();
            }
        } finally {
            MemoryUtil.memFree(error);
        }
    }

    public void play() throws Exception {
        ShortBuffer buf = MemoryUtil.memAllocShort(4096 * channels);
        try {
            while (!queuedToStop) {
                pcm.applyGain();
                buf.clear();
                int samples = STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, buf);
                if (samples <= 0) break;

                int shorts = samples * channels;
                if (scratch.length < shorts) {
                    scratch = new short[shorts];
                }
                buf.position(0);
                buf.limit(shorts);
                buf.get(scratch, 0, shorts);
                pcm.write(scratch, 0, shorts);
            }
        } finally {
            complete = true;
            MemoryUtil.memFree(buf);
            close();
        }
    }

    public void close() {
        if (handle != 0) {
            try {
                pcm.close(!queuedToStop);
            } catch (Exception ignored) {
            }
            try {
                STBVorbis.stb_vorbis_close(handle);
            } catch (Exception ignored) {
            }
            handle = 0;
        }
        if (fileData != null) {
            try {
                MemoryUtil.memFree(fileData);
            } catch (Exception ignored) {
            }
            fileData = null;
        }
    }
}
