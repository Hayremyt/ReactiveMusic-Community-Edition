package circuitlord.reactivemusic.entries;

import java.util.Locale;

/** What the mob's target should be for the condition to match. */
public enum RMEntityTargetMode {
    /** Target state doesn't matter. */
    ANY,
    /** The mob is targeting the player. */
    TARGET_ME,
    /** The mob is targeting someone (or something) that is not the player. */
    TARGET_OTHER;

    /** Parses the value of the "target=" parameter, null when unknown. */
    public static RMEntityTargetMode parse(String value) {
        if (value == null) return null;
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "me":
            case "player":
                return TARGET_ME;
            case "other":
            case "someone":
                return TARGET_OTHER;
            case "any":
                return ANY;
            default:
                return null;
        }
    }
}
