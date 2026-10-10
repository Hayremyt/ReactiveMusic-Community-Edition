package circuitlord.reactivemusic;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

/**
 * Shared 16-bit PCM output for non-MP3 formats (WAV via JavaSound, OGG via stb_vorbis).
 * No Minecraft classes, safe on any version/loader.
 */
public class PcmPlayer {

    /** Live game gain in dB, pushed by PlayerThread.processRealGain every tick. */
    public volatile static float gameGainDb = 0f;

    private SourceDataLine line;
    private byte[] scratch = new byte[0];

    public PcmPlayer(int sampleRate, int channels) throws Exception {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate, 16, channels, channels * 2, sampleRate, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
    }

    public boolean isOpen() {
        return line != null && line.isOpen();
    }

    public void setGainDb(float gain) {
        if (line == null) return;
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float clamped = Math.min(gainControl.getMaximum(), Math.max(gainControl.getMinimum(), gain));
                gainControl.setValue(clamped);
            }
        } catch (Exception ignored) {
        }
    }

    /** Picks up the live game gain (volume options, fades, ducking). */
    public void applyGain() {
        setGainDb(gameGainDb);
    }

    /** Writes little-endian 16-bit samples. */
    public void write(short[] samples, int offset, int length) {
        if (line == null) return;
        int needed = length * 2;
        if (scratch.length < needed) {
            scratch = new byte[needed];
        }
        for (int i = 0; i < length; i++) {
            short s = samples[offset + i];
            scratch[i * 2] = (byte) (s & 0xff);
            scratch[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        line.write(scratch, 0, needed);
    }

    /** Writes raw little-endian 16-bit bytes. */
    public void writeBytes(byte[] bytes, int offset, int length) {
        if (line == null) return;
        line.write(bytes, offset, length);
    }

    public void close(boolean drain) {
        if (line == null) return;
        try {
            if (drain) line.drain();
            else line.flush();
        } catch (Exception ignored) {
        }
        try {
            line.stop();
        } catch (Exception ignored) {
        }
        try {
            line.close();
        } catch (Exception ignored) {
        }
        line = null;
    }
}
