package circuitlord.reactivemusic;

public enum SongpackEventType {

    NONE,

    MAIN_MENU,
    CREDITS,


    HOME,


    // --- TIME ---
    DAY,
    NIGHT,
    SUNRISE,
    SUNSET,

    // --- Weather ---
    RAIN,
    SNOW,
    STORM,


    // --- world height ---
    UNDERWATER,
    UNDERGROUND,
    DEEP_UNDERGROUND,
    HIGH_UP,

    // --- Entities ---
    MINECART,
    BOAT,
    HORSE,
    PIG,

    //Actions
    FISHING,
    DYING,


    // TODO: remove
    OVERWORLD,
    NETHER,
    END,

    // MOBS

    BOSS,
    VILLAGE,

    NEARBY_MOBS,

    // entity-based location/boss tags (from nearby mobs, see issue #26)
    NETHER_FORTRESS,
    BASTION,
    END_CITY,
    OCEAN_TEMPLE,
    FARM,
    RAID,

    WARDEN,
    WITHER,
    DRAGON,
    BATTLE,
    COMBAT,




    GENERIC

}

