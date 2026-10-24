package circuitlord.reactivemusic.client.handshake;

/**
 * Общие константы протокола рукопожатия между клиентом и сервером ReactiveMusic.
 */
public final class HandshakeConstants {

    public static final long HANDSHAKE_TIMEOUT_MS = 5000L;
    public static final long HANDSHAKE_RETRY_MS = 1000L;
    public static final String CHANNEL_ID = "reactivemusic:server";

    private HandshakeConstants() {}
}
