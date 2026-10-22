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
}
