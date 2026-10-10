package circuitlord.reactivemusic;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * WAV playback through the JDK's built-in decoder, rendered on a shared PcmPlayer.
 * No Minecraft classes, safe on any version/loader.
 */
public class WavPlayer {

    public volatile boolean queuedToStop = false;

    private volatile boolean complete = false;
    private AudioInputStream pcmStream;
    private PcmPlayer pcm;

    public boolean isDone() {
        return complete;
    }

    public WavPlayer(InputStream in) throws Exception {
        // AudioSystem needs mark/reset, zip/resource streams don't have it
        AudioInputStream raw = AudioSystem.getAudioInputStream(new BufferedInputStream(in));

        AudioFormat base = raw.getFormat();
        AudioInputStream decoded;
        if (base.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && base.getSampleSizeInBits() == 16) {
            decoded = raw;
        } else {
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(), 16, base.getChannels(), base.getChannels() * 2,
                    base.getSampleRate(), false);
            decoded = AudioSystem.getAudioInputStream(target, raw);
        }

        AudioFormat fmt = decoded.getFormat();
        pcmStream = decoded;
        pcm = new PcmPlayer((int) fmt.getSampleRate(), fmt.getChannels());
    }

    public void play() throws Exception {
        byte[] buf = new byte[8192];
        try {
            int n;
            while (!queuedToStop && (n = pcmStream.read(buf)) != -1) {
                pcm.applyGain();
                // 16-bit frames only, drop a trailing odd byte if the file is malformed
                pcm.writeBytes(buf, 0, n - (n % 2));
            }
        } finally {
            complete = true;
            try {
                pcmStream.close();
            } catch (Exception ignored) {
            }
            pcm.close(!queuedToStop);
        }
    }
}
