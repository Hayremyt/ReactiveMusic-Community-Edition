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

    // the way conditions work just means that each condition requires there to be at least one true in each list (or empty list) for the whole condition to be valid
    // This is how we handle ORs

    public List<SongpackEventType> songpackEvents = new ArrayList<>();
    public List<String> biomeTypes = new ArrayList<>();

    public List<String> dimTypes = new ArrayList<>();

    public List<TagKey<Biome>> biomeTags = new ArrayList<>();

    public List<RMEntryBlockCondition> blocks = new ArrayList<>();

    public List<String> structureTypes = new ArrayList<>();

    public List<RMEntryEntityCondition> entityTypes = new ArrayList<>();

    public List<String> bossTypes = new ArrayList<>();

    // negated ("!EVENT") lists, none of these may match for the condition to be valid
    public List<SongpackEventType> negSongpackEvents = new ArrayList<>();
    public List<String> negBiomeTypes = new ArrayList<>();

    public List<String> negDimTypes = new ArrayList<>();

    public List<TagKey<Biome>> negBiomeTags = new ArrayList<>();

    public List<RMEntryBlockCondition> negBlocks = new ArrayList<>();

    public List<String> negStructureTypes = new ArrayList<>();

    public List<RMEntryEntityCondition> negEntityTypes = new ArrayList<>();

    public List<String> negBossTypes = new ArrayList<>();

}
