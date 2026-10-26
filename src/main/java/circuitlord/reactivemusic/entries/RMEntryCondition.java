package circuitlord.reactivemusic.entries;

import circuitlord.reactivemusic.SongpackEventType;
//? if >=1.20 {
/*import net.minecraft.registry.tag.TagKey;
*///?} else {
import net.minecraft.tag.TagKey;
//?}
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class RMEntryCondition {

    // One "||" alternative of an event string (see RMEntryConditionGroup). It holds when at
    // least one positive term matches (empty list = no constraint) AND no negated term matches.
    // Grouping is what makes "!X" inside a "||" behave as "OR NOT" rather than "AND NOT".

    public List<SongpackEventType> songpackEvents = new ArrayList<>();
    public List<String> biomeTypes = new ArrayList<>();

    public List<String> dimTypes = new ArrayList<>();

    public List<TagKey<Biome>> biomeTags = new ArrayList<>();

    public List<RMEntryBlockCondition> blocks = new ArrayList<>();

    public List<String> structureTypes = new ArrayList<>();

    public List<RMEntryEntityCondition> entityTypes = new ArrayList<>();

    public List<RMEntryBossCondition> bossTypes = new ArrayList<>();

    // negated ("!EVENT") lists, none of these may match for the condition to be valid
    public List<SongpackEventType> negSongpackEvents = new ArrayList<>();
    public List<String> negBiomeTypes = new ArrayList<>();

    public List<String> negDimTypes = new ArrayList<>();

    public List<TagKey<Biome>> negBiomeTags = new ArrayList<>();

    public List<RMEntryBlockCondition> negBlocks = new ArrayList<>();

    public List<String> negStructureTypes = new ArrayList<>();

    public List<RMEntryEntityCondition> negEntityTypes = new ArrayList<>();

    public List<RMEntryBossCondition> negBossTypes = new ArrayList<>();

}
