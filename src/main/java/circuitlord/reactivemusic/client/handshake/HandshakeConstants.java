package circuitlord.reactivemusic.client.handshake;

/**
 * Общие константы протокола рукопожатия между клиентом и сервером ReactiveMusic.
 */
public final class HandshakeConstants {

    public static final long HANDSHAKE_TIMEOUT_MS = 5000L;
    public static final long HANDSHAKE_RETRY_MS = 1000L;

    /** Клиент ↔ Сервер: рукопожатие (HandshakeRequest / HandshakeResponse). */
    public static final String CHANNEL_ID = "reactivemusic:server";

    /** Сервер → Клиент: MobTargetPacket (точные цели мобов от серверного мода). */
    public static final String CHANNEL_TARGETS = "reactivemusic:server/targets";

    /** Сервер → Клиент: StructurePacket (структуры вокруг игрока от серверного мода). */
    public static final String CHANNEL_STRUCTURES = "reactivemusic:server/structures";

    private HandshakeConstants() {}
}
