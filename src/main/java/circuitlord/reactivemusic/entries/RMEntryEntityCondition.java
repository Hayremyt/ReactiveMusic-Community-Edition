package circuitlord.reactivemusic.entries;

public class RMEntryEntityCondition {

    public String entity = "";

    /** Minimum amount of matching mobs. */
    public int minCount = 1;

    /** What the mob's target has to be. */
    public RMEntityTargetMode targetMode = RMEntityTargetMode.ANY;

    // distance bounds in blocks, null means unbounded (below 40 = closer than 40, above 20 = farther than 20)
    public Float belowDistance = null;
    public Float aboveDistance = null;

    /**
     * Runtime only, never parsed from the pack. World tick of the last real match, used to keep the
     * condition true for a short grace period so a mob pacing on the distance boundary cannot make
     * the music flip back and forth.
     */
    public long lastMatchTick = Long.MIN_VALUE;
}
