package circuitlord.reactivemusic.entries;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.SongpackEntry;
import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.SongpackZip;
import circuitlord.reactivemusic.platform.BiomeTagHelper;
//? if >=1.20 {
/*import net.minecraft.registry.tag.TagKey;
*///?} else {
import net.minecraft.tag.TagKey;
//?}
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RMRuntimeEntry {


    public List<RMEntryCondition> conditions = new ArrayList<>();

    public String songpack;

    public boolean allowFallback = false;

    public boolean forceStopMusicOnValid = false;
    public boolean forceStopMusicOnInvalid = false;

    public boolean forceStartMusicOnValid = false;

    public float forceChance = 1.0f;

    public boolean ignoreMusicDelay = false;

    public boolean alwaysOverride = false;

    public List<String> songs = new ArrayList<>();

    public String eventString = "";

    public String errorString = "";

    public float cachedRandomChance = 1.0f;


    public static RMRuntimeEntry create(SongpackZip songpack, SongpackEntry songpackEntry) {

        RMRuntimeEntry Entry = new RMRuntimeEntry();
        Entry.songpack = songpack.config.name;// songpackName;

        Entry.allowFallback = songpackEntry.allowFallback;

        Entry.forceStopMusicOnValid = songpackEntry.forceStopMusicOnValid || songpackEntry.forceStopMusicOnChanged;
        Entry.forceStopMusicOnInvalid = songpackEntry.forceStopMusicOnInvalid || songpackEntry.forceStopMusicOnChanged;

        Entry.forceStartMusicOnValid = songpackEntry.forceStartMusicOnValid;

        Entry.forceChance = songpackEntry.forceChance;

        Entry.ignoreMusicDelay = songpackEntry.ignoreMusicDelay;

        Entry.alwaysOverride = songpackEntry.alwaysOverride;

        if (songpackEntry.songs != null) {
            Entry.songs = Arrays.stream(songpackEntry.songs).toList();
        }

        for (int i = 0; i < songpackEntry.events.length; i++) {
            Entry.eventString += songpackEntry.events[i] + "_";
        }

        for (String event : songpackEntry.events) {

            RMEntryCondition condition = new RMEntryCondition();

            String cleanedEvent = event.replaceAll("\\s", "");
            cleanedEvent = cleanedEvent.toLowerCase();

            // backwards compat with v0.5
            if (songpack.convertBiomeToBiomeTag) {
                cleanedEvent = cleanedEvent.replace("biome=", "biometag=");
            }

            // Split by "||"
            String[] eventSections = cleanedEvent.split("\\|\\|");

            boolean eventHasData = false;

            // Parse each event section (may only be one)
            for (String eventSection : eventSections) {

                // negated section ("!NIGHT", "!BIOME=ocean"), none of these may match
                boolean negated = false;
                if (eventSection.startsWith("!")) {
                    negated = true;
                    eventSection = eventSection.substring(1);
                    if (eventSection.isEmpty()) {
                        Entry.errorString += "Invalid syntax: lone '!' is not an event!\n\n";
                        continue;
                    }
                }

                if (eventSection.startsWith("block=")) {

                    String blockData = eventSection.substring(6);
                    String[] parts = blockData.split(",");

                    // name, optionally followed by the amount: BLOCK=nether_bricks,1000 or BLOCK=nether_bricks,c=1000
                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        RMEntryBlockCondition blockCond = new RMEntryBlockCondition();
                        blockCond.block = parts[0];
                        blockCond.requiredCount = 1;

                        if (parts.length >= 2 && !parts[1].isEmpty()) {
                            String amount = parts[1].trim();
                            String lower = amount.toLowerCase(java.util.Locale.ROOT);

                            if (lower.startsWith("c=") || lower.startsWith("count=")) {
                                amount = amount.substring(amount.indexOf('=') + 1).trim();
                            }

                            if (!amount.matches("\\d+")) {
                                Entry.errorString += "Invalid syntax: " + eventSection + "!\n\n";
                                continue;
                            }

                            blockCond.requiredCount = Integer.parseInt(amount);
                        }

                        if (negated) condition.negBlocks.add(blockCond);
                        else condition.blocks.add(blockCond);

                        eventHasData = true;
                    }
                    else {
                        Entry.errorString += "Invalid syntax: " + eventSection + "!\n\n";
                    }
                }

                // see if it's a biome event
                else if (eventSection.startsWith("biome=")) {

                    String biomeName = eventSection.substring(6);
                    if (biomeName.isEmpty())
                        continue;

                    if (negated) condition.negBiomeTypes.add(biomeName);
                    else condition.biomeTypes.add(biomeName);
                    eventHasData = true;
                }

                // Biome-tags
                else if (eventSection.startsWith("biometag=")) {

                    String rawTagString = eventSection.substring(9);
                    if (rawTagString.isEmpty())
                        continue;

                    String path = normalizeBiomeTagPath(rawTagString);
                    TagKey<Biome> biomeTag = BiomeTagHelper.INSTANCE.createBiomeTagKey(path);

                    if (biomeTag != null) {
                        if (negated) condition.negBiomeTags.add(biomeTag);
                        else condition.biomeTags.add(biomeTag);
                        SongPicker.registerBiomeTag(biomeTag);
                        eventHasData = true;
                    } else {
                        Entry.errorString += "Biome tag '" + rawTagString + "' is not available on this platform!\n\n";
                    }

                }

                // dimensions
                else if (eventSection.startsWith("dim=")) {
                    String dimName = eventSection.substring(4);
                    if (dimName.isEmpty())
                        continue;

                    if (negated) condition.negDimTypes.add(dimName);
                    else condition.dimTypes.add(dimName);
                    eventHasData = true;
                }

                // structures
                else if (eventSection.startsWith("structure=")) {
                    String structName = eventSection.substring(10);
                    if (structName.isEmpty())
                        continue;

                    if (negated) condition.negStructureTypes.add(structName);
                    else condition.structureTypes.add(structName);
                    eventHasData = true;
                }

                // structures (short alias)
                else if (eventSection.startsWith("struct=")) {
                    String structName = eventSection.substring(7);
                    if (structName.isEmpty())
                        continue;

                    if (negated) condition.negStructureTypes.add(structName);
                    else condition.structureTypes.add(structName);
                    eventHasData = true;
                }

                // entities, named parameters only: ENTITY=zombie,r=32,c=3,target=me
                // (this condition is new, so there is no legacy positional syntax to keep)
                else if (eventSection.startsWith("entity=")) {
                    String entityData = eventSection.substring(7);
                    String[] parts = entityData.split(",");

                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        RMEntryEntityCondition entityCond = new RMEntryEntityCondition();
                        entityCond.entity = parts[0];

                        boolean invalid = false;

                        for (int i = 1; i < parts.length; i++) {
                            String part = parts[i].trim();
                            if (part.isEmpty()) continue;

                            String lower = part.toLowerCase(java.util.Locale.ROOT);
                            int eq = lower.indexOf('=');

                            if (eq <= 0) {
                                Entry.errorString += "Invalid entity condition: " + eventSection + "! (use named parameters, e.g. r=32, c=3)\n\n";
                                invalid = true;
                                break;
                            }

                            String key = lower.substring(0, eq).trim();
                            String value = lower.substring(eq + 1).trim();

                            if (key.equals("r") || key.equals("radius")) {
                                if (!value.matches("\\d+")) {
                                    Entry.errorString += "Invalid entity condition: " + eventSection + " (bad radius \"" + value + "\")!\n\n";
                                    invalid = true;
                                    break;
                                }
                                entityCond.radius = Math.min(Integer.parseInt(value), SongPicker.ENTITY_SCAN_RADIUS);
                                continue;
                            }

                            if (key.equals("c") || key.equals("count")) {
                                if (!value.matches("\\d+")) {
                                    Entry.errorString += "Invalid entity condition: " + eventSection + " (bad count \"" + value + "\")!\n\n";
                                    invalid = true;
                                    break;
                                }
                                entityCond.minCount = Math.max(1, Integer.parseInt(value));
                                continue;
                            }

                            if (key.equals("target")) {
                                RMEntityTargetMode mode = RMEntityTargetMode.parse(value);
                                if (mode == null) {
                                    Entry.errorString += "Invalid entity condition: " + eventSection + " (bad target \"" + value + "\", use me/other)!\n\n";
                                    invalid = true;
                                    break;
                                }
                                entityCond.targetMode = mode;
                                continue;
                            }

                            Entry.errorString += "Invalid entity condition: " + eventSection + " (unknown parameter \"" + key + "\")!\n\n";
                            invalid = true;
                            break;
                        }

                        if (invalid) continue;

                        if (negated) condition.negEntityTypes.add(entityCond);
                        else condition.entityTypes.add(entityCond);

                        eventHasData = true;
                    }
                    else {
                        Entry.errorString += "Invalid syntax: " + eventSection + "!\n\n";
                    }
                }

                // specific bosses by boss bar title substring, BOSS=wither (bare BOSS still means any boss bar)
                // optional HP phase with named parameters: BOSS=wither,below=50 / BOSS=wither,above=80
                else if (eventSection.startsWith("boss=")) {
                    String bossData = eventSection.substring(5);
                    String[] parts = bossData.split(",");

                    if (parts.length >= 1 && !parts[0].isEmpty()) {
                        RMEntryBossCondition bossCond = new RMEntryBossCondition();
                        bossCond.name = parts[0];

                        boolean invalid = false;

                        for (int i = 1; i < parts.length; i++) {
                            String part = parts[i].trim();
                            if (part.isEmpty()) continue;

                            int eq = part.indexOf('=');
                            if (eq <= 0) {
                                Entry.errorString += "Invalid boss condition: " + eventSection + "! (use named parameters, e.g. below=50)\n\n";
                                invalid = true;
                                break;
                            }

                            String key = part.substring(0, eq).trim().toLowerCase(java.util.Locale.ROOT);
                            String value = part.substring(eq + 1).trim();

                            if (!value.matches("\\d+(\\.\\d+)?")) {
                                Entry.errorString += "Invalid boss condition: " + eventSection + " (bad value \"" + value + "\")!\n\n";
                                invalid = true;
                                break;
                            }

                            float percent = Float.parseFloat(value);

                            if (key.equals("below") || key.equals("hp-below")) {
                                bossCond.belowPercent = percent;
                                continue;
                            }

                            if (key.equals("above") || key.equals("hp-above")) {
                                bossCond.abovePercent = percent;
                                continue;
                            }

                            Entry.errorString += "Invalid boss condition: " + eventSection + " (unknown parameter \"" + key + "\")!\n\n";
                            invalid = true;
                            break;
                        }

                        if (invalid) continue;

                        if (negated) condition.negBossTypes.add(bossCond);
                        else condition.bossTypes.add(bossCond);
                        eventHasData = true;
                    }
                    else {
                        Entry.errorString += "Invalid syntax: " + eventSection + "!\n\n";
                    }
                }

                // songpack events
                else {
                    try {
                        // try to cast to SongpackEvent
                        // needs uppercase for enum names
                        SongpackEventType eventType = Enum.valueOf(SongpackEventType.class, eventSection.toUpperCase());

                        // it's a songpack event
                        if (eventType != SongpackEventType.NONE) {
                            if (negated) condition.negSongpackEvents.add(eventType);
                            else condition.songpackEvents.add(eventType);
                            eventHasData = true;
                            continue;
                        }
                    } catch (Exception e) {
                        Entry.errorString += "Could not find event with name " + eventSection + "!\n\n";
                        //e.printStackTrace();
                    }
                }
            }

            // --- If we didn't find any valid conditions, skip this ---
            if (!eventHasData) {
                continue;
            }

            Entry.conditions.add(condition);

        }

        return Entry;

    }


    public static String normalizeBiomeTagPath(String input) {
        String path = input.toLowerCase().trim();
        // Tags that don't start with a known prefix get "is_" prepended
        // This handles backward compat: "WASTELAND" -> "is_wasteland", "HOT" -> "is_hot"
        if (!path.startsWith("is_") && !path.startsWith("has_") && !path.startsWith("no_default_")
                && !path.startsWith("hidden_from_")) {
            path = "is_" + path;
        }
        return path;
    }


}



