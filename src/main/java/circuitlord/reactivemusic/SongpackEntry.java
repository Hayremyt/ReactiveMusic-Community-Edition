package circuitlord.reactivemusic;

import net.minecraft.loot.entry.TagEntry;
//? if >=1.20 {
/*import net.minecraft.registry.tag.TagKey;
*///?} else {
import net.minecraft.tag.TagKey;
//?}
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class SongpackEntry {


    // expands out into songpack events and biometag events
    public String[] events;


    public boolean allowFallback = false;

    // OnChanged just sets both Valid and Invalid versions to true
    public boolean forceStopMusicOnChanged = false;
    public boolean forceStopMusicOnValid = false;
    public boolean forceStopMusicOnInvalid = false;

    public boolean forceStartMusicOnValid = false;

    public float forceChance = 1.0f;

    // skip the musicDelayLength silence before starting this entry (good for BOSS/CREDITS with LONG delay)
    public boolean ignoreMusicDelay = false;

    // if valid, win over every entry without this flag regardless of file order (good for BOSS)
    public boolean alwaysOverride = false;

    public boolean startMusicOnEventValid = false;

    // deprecated for now
    public boolean stackable = false;

    public String[] songs;


    // deprecated
    public boolean alwaysPlay = false;
    public boolean alwaysStop = false;


}
