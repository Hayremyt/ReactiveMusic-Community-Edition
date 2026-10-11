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
    private int outChannels;
    private short[] scratch = new short[0];
    private short[] mixScratch = new short[0];

    public boolean isDone() {
        return complete;
    }

    public OggPlayer(InputStream in) throws Exception {
        try {
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
                    // sound hardware is stereo at best, downmix the rest
                    outChannels = Math.min(channels, 2);
                    pcm = new PcmPlayer(info.sample_rate(), outChannels);
                } finally {
                    info.free();
                }
            } finally {
                MemoryUtil.memFree(error);
            }
        } catch (Exception e) {
            close();
            throw e;
        }
    }

    /** Dual-mono average downmix, pure math, unit-testable without audio hardware. */
    public static void mixDownToStereo(short[] in, int channels, short[] out, int samples) {
        for (int i = 0; i < samples; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                sum += in[i * channels + c];
            }
            short mixed = (short) (sum / channels);
            out[i * 2] = mixed;
            out[i * 2 + 1] = mixed;
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

                if (channels <= 2) {
                    pcm.write(scratch, 0, shorts);
                } else {
                    int outShorts = samples * outChannels;
                    if (mixScratch.length < outShorts) {
                        mixScratch = new short[outShorts];
                    }
                    mixDownToStereo(scratch, channels, mixScratch, samples);
                    pcm.write(mixScratch, 0, outShorts);
                }
            }
        } finally {
            complete = true;
            MemoryUtil.memFree(buf);
            close();
        }
    }

    public void close() {
        if (handle != 0) {
            if (pcm != null) {
                try {
                    pcm.close(!queuedToStop);
                } catch (Exception ignored) {
                }
                pcm = null;
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
