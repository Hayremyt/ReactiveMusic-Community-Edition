package circuitlord.reactivemusic.entries;

import java.util.ArrayList;
import java.util.List;

/**
 * One event string from the {@code events} array, e.g. {@code "BIOME=ocean || !RAIN"}.
 * <p>
 * The string is split on {@code ||} into alternatives that are combined with <b>OR</b>:
 * the group holds when <b>any</b> of its alternatives holds. Different event strings of an
 * entry are combined with <b>AND</b> (see {@link RMRuntimeEntry}).
 * <p>
 * Grouping the alternatives (instead of flattening them into a single positive/negative set)
 * is what makes {@code !} inside a {@code ||} behave as "OR NOT" rather than "AND NOT".
 */
public class RMEntryConditionGroup {

    /** OR alternatives; the group holds if at least one of them holds. */
    public List<RMEntryCondition> alternatives = new ArrayList<>();

}
