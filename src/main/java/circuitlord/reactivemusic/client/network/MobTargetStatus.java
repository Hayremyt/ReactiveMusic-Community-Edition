package circuitlord.reactivemusic.client.network;

/**
 * Статус цели моба относительно локального игрока.
 * <p>
 * Совпадает по ordinal с серверным {@code circuitlord.reactivemusic.server.MobTargetStatus}.
 */
public enum MobTargetStatus {
    /** Ни на кого не нацелен / нерелевантен — «забудь». */
    NONE,
    /** Нацелен на локального игрока. */
    ME,
    /** Нацелен на кого-то другого. */
    OTHER;

    public static MobTargetStatus byOrdinal(int ordinal) {
        MobTargetStatus[] values = values();
        if (ordinal < 0 || ordinal >= values.length) return NONE;
        return values[ordinal];
    }
}
