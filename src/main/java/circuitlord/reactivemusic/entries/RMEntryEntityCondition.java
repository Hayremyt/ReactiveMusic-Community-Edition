package circuitlord.reactivemusic.entries;

public class RMEntryEntityCondition {

    public String entity = "";

    /** Radius in blocks, how close the mob has to be. */
    public int radius = 32;

    /** Minimum amount of matching mobs. */
    public int minCount = 1;

    /** What the mob's target has to be. */
    public RMEntityTargetMode targetMode = RMEntityTargetMode.ANY;
}
