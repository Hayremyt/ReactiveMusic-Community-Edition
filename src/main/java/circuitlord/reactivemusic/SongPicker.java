package circuitlord.reactivemusic;


import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.entries.RMEntryBlockCondition;
import circuitlord.reactivemusic.entries.RMEntryBossCondition;
import circuitlord.reactivemusic.entries.RMEntryCondition;
import circuitlord.reactivemusic.entries.RMEntryEntityCondition;
import circuitlord.reactivemusic.entries.RMEntityTargetMode;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.client.handshake.ClientHandshakeManager;
import circuitlord.reactivemusic.client.network.MobTargetStatus;
import circuitlord.reactivemusic.mixin.BossBarHudAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;

//? if >=1.20 {
/*import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
*///?} else {
import net.minecraft.tag.TagKey;
import net.minecraft.util.registry.Registry;
//?}
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

public final class SongPicker {



    public static Map<SongpackEventType, Boolean> songpackEventMap = new EnumMap<>(SongpackEventType.class);

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static Map<Entity, Long> recentEntityDamageSources = new HashMap<>();

    public static final long COMBAT_TIMEOUT = 200L;
    public static final int COMBAT_HIT_THRESHOLD = 3;
    public static final float COMBAT_HEAVY_HIT_FRACTION = 0.25f;

    /** Sentinel for "this never happened". */
    private static final long NEVER_HAPPENED = Long.MIN_VALUE;

    private static final List<Long> recentCombatHits = new ArrayList<>();
    private static long lastDamageTakenTime = NEVER_HAPPENED;
    private static long lastHeavyHitTime = NEVER_HAPPENED;
    /** Последнее известное здоровье игрока — для клиентского детекта урона на dédi сервере. */
    private static float lastPlayerHealth = -1.0f;

    public static void registerCombatHit() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return;
        recentCombatHits.add(mc.world.getTime());
    }

    public static void registerDamageTaken(float amount) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return;
        lastDamageTakenTime = mc.world.getTime();
        // single massive hit (creeper in the back, ghast fireball) counts as a fight on its own
        if (amount / mc.player.getMaxHealth() > COMBAT_HEAVY_HIT_FRACTION) {
            lastHeavyHitTime = mc.world.getTime();
        }
    }


    private static final Map<Block, String> BLOCK_ID_CACHE = new IdentityHashMap<>();

    /** Radius in blocks of the cube that is scanned for block counts. */
    public static final int BLOCK_SCAN_RADIUS = 25;

    public static boolean queuedToPrintBlockCounter = false;
    public static BlockPos cachedBlockCounterOrigin;
    // starts outside the scan range, so the first tick wraps around to -BLOCK_SCAN_RADIUS
    public static int currentBlockCounterX = BLOCK_SCAN_RADIUS + 1;
    private static World lastBlockScanWorld = null;

    public static Object2IntOpenHashMap<String> blockCounterMap = new Object2IntOpenHashMap<>();
    public static Object2IntOpenHashMap<String> cachedBlockChecker = new Object2IntOpenHashMap<>();

    public static String currentBiomeName = "";
    public static String currentDimName = "";

    public static Set<String> currentStructureNames = new HashSet<>();

    public static Set<String> currentBossNames = new HashSet<>();

    // normalized boss bar title -> lowest percent (0-1) among matching bars
    public static Map<String, Float> currentBossPercents = new HashMap<>();

    public static final int ENTITY_SCAN_RADIUS = 64;
    /**
     * How long an ENTITY= condition stays true after it last matched, in ticks (1s = 20).
     * Without this a mob pacing on a below=/above= boundary flips the condition every tick.
     */
    public static final long ENTITY_CONDITION_GRACE_TICKS = 40L;

    /** Cosine threshold for "the mob is looking at us"; 0.8 is roughly a 37 degree cone. */
    private static final double TARGET_FACING_DOT = 0.8;
    /** Only trust the look/approach test at close range, further out it gets unreliable. */
    private static final double TARGET_FACING_RANGE = 24.0;
    /** Warden anger value at which it starts actively hunting (vanilla: 80 = agitated). */
    private static final int WARDEN_HUNTING_ANGER = 80;
    public static List<Entity> nearbyEntities = new ArrayList<>();
    public static List<String> nearbyEntityIds = new ArrayList<>();
    public static BlockPos entityScanCenter = BlockPos.ORIGIN;
    /** World tick of the last entity scan, the grace window is measured against it. */
    public static long entityScanCenterTick = 0L;
    /** Клиентский менеджер рукопожатия с серверным модом. */
    public static ClientHandshakeManager clientHandshakeManager = new ClientHandshakeManager();

    /** Статусы целей мобов от серверного мода (dedicated server). UUID моба -> статус. */
    public static final Map<UUID, MobTargetStatus> serverMobTargets = new ConcurrentHashMap<>();

    /** Структуры от серверного мода (dedicated server). */
    public static final Set<String> serverStructureNames = ConcurrentHashMap.newKeySet();


    private static final Random rand = new Random();

    private static List<String> recentlyPickedSongs = new ArrayList<>();

    public static Long TIME_FOR_FORGET_DAMAGE_SOURCE = 200L;

    public static boolean wasSleeping = false;

    public static void registerBiomeTag(TagKey<Biome> tag) {
        if (!biomeTagEventMap.containsKey(tag)) {
            biomeTagEventMap.put(tag, false);
        }
    }




    public static void tickEventMap() {

        currentBiomeName = "";
        currentDimName = "";
        currentStructureNames.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null)
            return;

        ClientPlayerEntity player = mc.player;
        World world = mc.world;


        songpackEventMap.put(SongpackEventType.MAIN_MENU, player == null || world == null);
        songpackEventMap.put(SongpackEventType.CREDITS, mc.currentScreen instanceof CreditsScreen);

        // Early out if not in-game
        if (player == null || world == null) {
            // drop per-world state so menu music never keys off stale entities/combat
            nearbyEntities.clear();
            nearbyEntityIds.clear();
            currentBossNames.clear();
            recentCombatHits.clear();
            lastDamageTakenTime = NEVER_HAPPENED;
            lastHeavyHitTime = NEVER_HAPPENED;
            serverMobTargets.clear();
            serverStructureNames.clear();
            return;
        }

        // World processing
        BlockPos playerPos = new BlockPos(player.getBlockPos());
        var biome = world.getBiome(playerPos);

        // Copied logic out from getIdAsString
        currentBiomeName = (String)biome.getKey().map((key) -> {
            return key.getValue().toString();
        }).orElse("[unregistered]");

        boolean underground = !world.isSkyVisible(playerPos);
        var indimension = world.getRegistryKey();

        currentDimName = indimension.getValue().toString();

        tickStructures(world, playerPos);

        Entity riding = VersionHelper.GetRidingEntity(player);

        long time = world.getTimeOfDay() % 24000;
        boolean night = time >= 13000 && time < 23000;
        boolean sunset = time >= 12000 && time < 13000;
        boolean sunrise = time >= 23000;


        // TODO: someone help me I have no idea how to get the name of the world/server but if you know how then put it instead of "saved"
        if (!wasSleeping && player.isSleeping()) {
            //? if >=1.21.9 {
            /*ReactiveMusic.config.savedHomePositions.put("saved", player.getEntityPos());
            *///?} else {
            ReactiveMusic.config.savedHomePositions.put("saved", player.getPos());
            //?}

            ModConfig.saveConfig();
        }

        wasSleeping = player.isSleeping();


        // special

        if (ReactiveMusic.config.savedHomePositions.containsKey("saved")) {

            //? if >=1.21.9 {
            /*Vec3d dist = player.getEntityPos().subtract(ReactiveMusic.config.savedHomePositions.get("saved"));
            *///?} else {
            Vec3d dist = player.getPos().subtract(ReactiveMusic.config.savedHomePositions.get("saved"));
            //?}

            songpackEventMap.put(SongpackEventType.HOME, dist.length() < 45.0f);
        }
        else {
            songpackEventMap.put(SongpackEventType.HOME, false);
        }



        // Time
        songpackEventMap.put(SongpackEventType.DAY, !night);
        songpackEventMap.put(SongpackEventType.NIGHT, night);
        songpackEventMap.put(SongpackEventType.SUNSET, sunset);
        songpackEventMap.put(SongpackEventType.SUNRISE, sunrise);


        // Actions

        songpackEventMap.put(SongpackEventType.DYING, player.getHealth() / player.getMaxHealth() < 0.35);
        songpackEventMap.put(SongpackEventType.FISHING, player.fishHook != null);

        songpackEventMap.put(SongpackEventType.MINECART, riding instanceof MinecartEntity);
        songpackEventMap.put(SongpackEventType.BOAT, riding instanceof BoatEntity);
        songpackEventMap.put(SongpackEventType.HORSE, riding instanceof HorseEntity);
        songpackEventMap.put(SongpackEventType.PIG, riding instanceof PigEntity);


        songpackEventMap.put(SongpackEventType.OVERWORLD, indimension == World.OVERWORLD);
        songpackEventMap.put(SongpackEventType.NETHER, indimension == World.NETHER);
        songpackEventMap.put(SongpackEventType.END, indimension == World.END);


        songpackEventMap.put(SongpackEventType.UNDERGROUND, indimension == World.OVERWORLD && underground && playerPos.getY() < ReactiveMusic.config.undergroundMaxY);
        songpackEventMap.put(SongpackEventType.DEEP_UNDERGROUND, indimension == World.OVERWORLD && underground && playerPos.getY() < ReactiveMusic.config.deepUndergroundMaxY);
        songpackEventMap.put(SongpackEventType.HIGH_UP, indimension == World.OVERWORLD && !underground && playerPos.getY() > ReactiveMusic.config.highUpMinY);

        songpackEventMap.put(SongpackEventType.UNDERWATER, player.isSubmergedInWater());

        // Weather
        //? if >=1.21.9 {
        /*songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitation(playerPos, world.getSeaLevel()) == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitation(playerPos, world.getSeaLevel()) == Biome.Precipitation.SNOW);
        *///?} else if >=1.20 {
        /*songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitation(playerPos) == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitation(playerPos) == Biome.Precipitation.SNOW);
        *///?} else {
        songpackEventMap.put(SongpackEventType.RAIN, world.isRaining() && biome.value().getPrecipitation() == Biome.Precipitation.RAIN);
        songpackEventMap.put(SongpackEventType.SNOW, world.isRaining() && biome.value().getPrecipitation() == Biome.Precipitation.SNOW);
        //?}

        songpackEventMap.put(SongpackEventType.STORM, world.isThundering());


        var currentTags = biome.streamTags().toList();

        // Update all registered biome tags
        for (TagKey<Biome> tag : biomeTagEventMap.keySet()) {
            boolean found = false;

            for (TagKey<Biome> curTag : currentTags) {
                if (curTag.id().equals(tag.id())) {
                    found = true;
                    break;
                }
            }

            biomeTagEventMap.put(tag, found);
        }


        // process recent damage sources

        // remove past sources
        recentEntityDamageSources.entrySet().removeIf(entry -> entry.getKey() == null || !entry.getKey().isAlive() || world.getTime() - entry.getValue() > TIME_FOR_FORGET_DAMAGE_SOURCE);

        // add new damage sources — prefer the real attacker (e.g. skeleton) over the projectile (arrow)
        var recentDamage = player.getRecentDamageSource();

        if (recentDamage != null) {
            Entity attacker = recentDamage.getAttacker();
            if (attacker == null) {
                attacker = recentDamage.getSource();
            }
            if (attacker != null) {
                recentEntityDamageSources.put(attacker, world.getTime());
            }
        }




        // Search for nearby entities that could be relevant to music

        {
            int villagerCount = 0;

            double radiusXZ = 30.0;
            double radiusY = 15.0;

            Box box = new Box(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                    player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

            List<VillagerEntity> nearbyVillagerCheck = world.getEntitiesByClass(VillagerEntity.class, box, entity -> entity != null);

            for (VillagerEntity villagerEntity : nearbyVillagerCheck) {
                villagerCount++;
            }

            songpackEventMap.put(SongpackEventType.VILLAGE, villagerCount > 0);

        }

        {
            List<HostileEntity> nearbyHostile = world.getEntitiesByClass(HostileEntity.class,
                    GetBoxAroundPlayer(player, 12.f, 6.f),
                    entity -> entity != null);

            songpackEventMap.put(SongpackEventType.NEARBY_MOBS, nearbyHostile.size() >= 1);

        }

        {
            // one shared scan for all ENTITY= conditions, individual radii/counts filter below
            // the box uses the same radius on Y so the spherical 64-block check is the only real limit
            List<Entity> found = world.getEntitiesByClass(Entity.class,
                    GetBoxAroundPlayer(player, ENTITY_SCAN_RADIUS, (float) ENTITY_SCAN_RADIUS),
                    entity -> entity != null && entity.isAlive() && entity != player);

            nearbyEntities = found;
            nearbyEntityIds = new ArrayList<>(found.size());
            entityScanCenter = playerPos;
            entityScanCenterTick = world.getTime();

            for (var e : found) {
                //? if >=1.20 {
                /*nearbyEntityIds.add(Registries.ENTITY_TYPE.getId(e.getType()).toString());
                *///?} else {
                nearbyEntityIds.add(Registry.ENTITY_TYPE.getId(e.getType()).toString());
                //?}
            }
        }

        {
            // entity-based location/boss tags, all derived from the single scan above
            songpackEventMap.put(SongpackEventType.NETHER_FORTRESS, hasNearbyEntity("wither_skeleton", "blaze"));
            songpackEventMap.put(SongpackEventType.BASTION, hasNearbyEntity("piglin_brute"));
            songpackEventMap.put(SongpackEventType.END_CITY, hasNearbyEntity("shulker"));
            songpackEventMap.put(SongpackEventType.OCEAN_TEMPLE, hasNearbyEntity("guardian")
                    && Boolean.TRUE.equals(songpackEventMap.get(SongpackEventType.UNDERWATER)));
            songpackEventMap.put(SongpackEventType.FARM, hasNearbyEntity("cow") && hasNearbyEntity("chicken")
                    && hasNearbyEntity("sheep") && hasNearbyEntity("pig"));
            songpackEventMap.put(SongpackEventType.RAID, hasNearbyEntity("pillager", "vindicator", "evoker", "ravager", "illusioner"));

            songpackEventMap.put(SongpackEventType.WARDEN, hasNearbyEntity("warden"));
            songpackEventMap.put(SongpackEventType.WITHER, hasNearbyEntityIdEnding(":wither"));
            songpackEventMap.put(SongpackEventType.DRAGON, hasNearbyEntity("ender_dragon"));
            songpackEventMap.put(SongpackEventType.BATTLE, recentEntityDamageSources.size() >= 5);
        }

        {
            // real fight: enough back-and-forth hits recently, plus actual danger
            // (recently hurt or hostile mobs nearby, so poking one zombie doesn't count)
            recentCombatHits.removeIf(hitTime -> world.getTime() - hitTime > COMBAT_TIMEOUT);
            boolean combatHits = recentCombatHits.size() >= COMBAT_HIT_THRESHOLD
                    || world.getTime() - lastHeavyHitTime <= COMBAT_TIMEOUT;
            boolean combatDanger = world.getTime() - lastDamageTakenTime <= COMBAT_TIMEOUT
                    || Boolean.TRUE.equals(songpackEventMap.get(SongpackEventType.NEARBY_MOBS));
            boolean peacefulMode = player.getAbilities().creativeMode || player.isSpectator();
            songpackEventMap.put(SongpackEventType.COMBAT, !peacefulMode && combatHits && combatDanger);
        }


        //songpackEventMap.put(SongpackEventType.HOSTILE_MOBS, aggroMobsCount >= 4);

        //System.out.println("Villager count: " + villagerCount + ", Aggro mobs count: " + aggroMobsCount);


        // try to get boss bars
        boolean bossBarActive = false;
        currentBossNames.clear();
        currentBossPercents.clear();

        if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            try {

                var bossBars = ((BossBarHudAccessor) mc.inGameHud.getBossBarHud()).getBossBars();

                if (!bossBars.isEmpty()) {
                    bossBarActive = true;
                    for (var bossBar : bossBars.values()) {
                        // strip whitespace, queries are whitespace-stripped too
                        String bossName = bossBar.getName().getString().toLowerCase(Locale.ROOT).replaceAll("\\s", "");
                        currentBossNames.add(bossName);
                        float percent = bossBar.getPercent();
                        Float known = currentBossPercents.get(bossName);
                        if (known == null || percent < known) {
                            currentBossPercents.put(bossName, percent);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }


        songpackEventMap.put(SongpackEventType.BOSS, bossBarActive);


        songpackEventMap.put(SongpackEventType.GENERIC, true);
    }


    public static void tickStructures(World world, BlockPos playerPos) {
        currentStructureNames.clear();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        // Path 1: integrated server (singleplayer / LAN) — read structures directly from the
        // integrated server world. No server mod needed; mc.getServer() is available here.
        var integratedServer = mc.getServer();
        if (integratedServer != null) {
            try {
                var serverWorld = integratedServer.getWorld(world.getRegistryKey());
                if (serverWorld == null) return;

                //? if >=1.21.11 {
                /*var structureRegistry = serverWorld.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
                *///?} else if >=1.20 {
                /*var structureRegistry = serverWorld.getRegistryManager().get(RegistryKeys.STRUCTURE);
                *///?} else {
                var structureRegistry = serverWorld.getRegistryManager().get(Registry.STRUCTURE_KEY);
                //?}

                // Exact: ask the structure accessor which structure actually contains the player.
                // This handles huge structures (mansion, ancient city) whose StructureStart is stored
                // many chunks away from the player's position.
                var accessor = serverWorld.getStructureAccessor();
                for (var entry : structureRegistry.getEntrySet()) {
                    var structure = entry.getValue();
                    var start = accessor.getStructureContaining(playerPos, structure);
                    if (start == null || !start.hasChildren()) continue;
                    if (!start.getBoundingBox().contains(playerPos)) continue;
                    var id = structureRegistry.getId(structure);
                    if (id != null) {
                        currentStructureNames.add(id.toString().toLowerCase(Locale.ROOT));
                    }
                }
            } catch (Exception ignored) {
                // never let structure lookup break the music tick
            }
            return;
        }

        // Path 2: dedicated server with server mod — use structure data received via network packet.
        if (clientHandshakeManager.isAcknowledged()) {
            currentStructureNames.addAll(serverStructureNames);
            return;
        }

        // Path 3: dedicated server without server mod — STRUCT stays false.
    }

    /**
     * Called from the loader-specific network handler when a {@code MobTargetPacket} arrives
     * from the server mod (dedicated server only).
     *
     * @param mobUuid UUID of the mob whose target status changed
     * @param status  ME / OTHER / NONE relative to the local player
     */
    public static void onMobTargetPacket(UUID mobUuid, MobTargetStatus status) {
        if (status == null || status == MobTargetStatus.NONE) {
            serverMobTargets.remove(mobUuid);
        } else {
            serverMobTargets.put(mobUuid, status);
        }
    }

    /**
     * Called from the loader-specific network handler when a {@code StructurePacket} arrives
     * from the server mod (dedicated server only).
     *
     * @param structures array of structure IDs currently around the player
     */
    public static void onStructurePacket(String[] structures) {
        serverStructureNames.clear();
        for (String s : structures) {
            if (s != null) {
                serverStructureNames.add(s.toLowerCase(Locale.ROOT));
            }
        }
    }


    public static void tickBlockCounterMap() {

        final int RADIUS = BLOCK_SCAN_RADIUS;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        World world = mc.world;
        if (player == null || world == null)
            return;

        // повторяем рукопожатие, пока сервер не ответил — чтобы запрос/ответ не потерялись
        clientHandshakeManager.tickRetry();

        // a fresh world (new game, rejoin, dimension change) invalidates the previous snapshot.
        // Reset BEFORE damage detection so a different health on the new world is not mistaken for damage.
        if (world != lastBlockScanWorld) {
            lastBlockScanWorld = world;
            blockCounterMap.clear();
            cachedBlockChecker.clear();
            cachedBlockCounterOrigin = null;
            currentBlockCounterX = BLOCK_SCAN_RADIUS + 1;
            lastPlayerHealth = -1.0f; // reset health baseline on world change
        }

        // client-side damage detection: if health dropped this tick, we took damage
        // (works on dedicated servers where PlayerEntity.damage() mixin may not fire)
        float currentHealth = player.getHealth();
        if (lastPlayerHealth >= 0.0f && currentHealth < lastPlayerHealth) {
            registerDamageTaken(lastPlayerHealth - currentHealth);
        }
        lastPlayerHealth = currentHealth;

        // advance one column of the cube per tick
        currentBlockCounterX++;
        if (currentBlockCounterX > RADIUS) {
            currentBlockCounterX = -RADIUS;
        }

        // finished iterating, reset
        if (currentBlockCounterX == -RADIUS) {
            // ReactiveMusic.LOGGER.info("Finished checking for blocks, resetting! Total: " + blockCounterMap.size());

            if (queuedToPrintBlockCounter) {

                player.sendMessage(Text.of("[ReactiveMusic]: Logging Block Counter map! Radius: " + RADIUS), false);

                blockCounterMap.object2IntEntrySet().stream()
                        .sorted((first, second) -> Integer.compare(second.getIntValue(), first.getIntValue()))
                        .forEach(entry -> player.sendMessage(Text.of(entry.getKey() + ": " + entry.getIntValue()), false));

                queuedToPrintBlockCounter = false;

            }

            Object2IntOpenHashMap<String> completedScan = cachedBlockChecker;
            cachedBlockChecker = blockCounterMap;
            blockCounterMap = completedScan;
            blockCounterMap.clear();
            cachedBlockCounterOrigin = player.getBlockPos();

        }

        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int y = -RADIUS; y <= RADIUS; y++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {

                // don't allocate new blockpos everytime
                mutablePos.set(
                        cachedBlockCounterOrigin.getX() + currentBlockCounterX,
                        cachedBlockCounterOrigin.getY() + y,
                        cachedBlockCounterOrigin.getZ() + z
                );


                Block block = world.getBlockState(mutablePos).getBlock();
                String key = getBlockId(block);

                if (isBlacklisted(key))
                    continue;

                blockCounterMap.addTo(key, 1);

            }
        }


    }



    /** Block ids containing any of the configured blacklist entries are not counted. */
    private static boolean isBlacklisted(String blockId) {
        var blacklist = ReactiveMusic.config != null ? ReactiveMusic.config.blockCounterBlacklist : null;
        if (blacklist == null || blacklist.isEmpty()) return false;

        for (String black : blacklist) {
            if (black != null && !black.isEmpty() && blockId.contains(black)) {
                return true;
            }
        }
        return false;
    }

    private static String getBlockId(Block block) {
        String id = BLOCK_ID_CACHE.get(block);
        if (id != null) return id;

        //? if >=1.20 {
        /*id = Registries.BLOCK.getId(block).toString();
        *///?} else {
        id = Registry.BLOCK.getId(block).toString();
        //?}
        BLOCK_ID_CACHE.put(block, id);
        return id;
    }


    private static Box GetBoxAroundPlayer(ClientPlayerEntity player, float radiusXZ, float radiusY) {

        return new Box(player.getX() - radiusXZ, player.getY() - radiusY, player.getZ() - radiusXZ,
                player.getX() + radiusXZ, player.getY() + radiusY, player.getZ() + radiusXZ);

    }


    public static void initialize() {

        songpackEventMap.clear();
        biomeTagEventMap.clear();

        for (SongpackEventType eventType : SongpackEventType.values()) {
            songpackEventMap.put(eventType, false);
        }
    }







    static boolean hasSongNotPlayedRecently(List<String> songs) {
        for (String song : songs) {
            if (!recentlyPickedSongs.contains(song)) {
                return true;
            }
        }
        return false;
    }


    static List<String> getNotRecentlyPlayedSongs(String[] songs) {
        List<String> notRecentlyPlayed = new ArrayList<>(Arrays.asList(songs));
        notRecentlyPlayed.removeAll(recentlyPickedSongs);
        return notRecentlyPlayed;
    }


    static void markSongRecent(String song) {
        if (song == null) return;
        // only track the past X songs
        if (recentlyPickedSongs.size() >= 8) {
            recentlyPickedSongs.remove(0);
        }
        if (!recentlyPickedSongs.contains(song)) {
            recentlyPickedSongs.add(song);
        }
    }

    static String pickRandomSong(List<String> songs) {
        if (songs.isEmpty()) {
            return null;
        }

        List<String> cleanedSongs = new ArrayList<>(songs);

        cleanedSongs.removeAll(recentlyPickedSongs);


        String picked;

        // If there's remaining songs, pick one of those
        if (!cleanedSongs.isEmpty()) {
            int randomIndex = rand.nextInt(cleanedSongs.size());
            picked = cleanedSongs.get(randomIndex);
        }

        // Else we've played all these recently so just pick a new random one
        else {
            int randomIndex = rand.nextInt(songs.size());
            picked = songs.get(randomIndex);
        }


        // only track the past X songs
        if (recentlyPickedSongs.size() >= 8) {
            recentlyPickedSongs.remove(0);
        }

        recentlyPickedSongs.add(picked);


        return picked;
    }


    public static String getSongName(String song) {
        return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
    }

    
    public static boolean isEntryValid(RMRuntimeEntry entry) {

        for (var group : entry.conditionGroups) {

            // every "events" string (group) must hold (AND) ...
            // ... and a group holds when ANY of its "||" alternatives holds (OR)
            boolean groupHolds = false;
            for (var condition : group.alternatives) {
                if (conditionMatches(condition)) {
                    groupHolds = true;
                    break;
                }
            }

            if (!groupHolds) return false;

        }

        // we passed without failing so it must be true
        return true;

    }

    /**
     * One {@code ||} alternative: it holds when its positives OR together and none of its
     * negated terms match. Keeping each alternative separate is what makes {@code !} inside
     * a {@code ||} behave as "OR NOT" instead of "AND NOT".
     */
    static boolean conditionMatches(RMEntryCondition condition) {

        boolean hasPositives = !condition.songpackEvents.isEmpty() || !condition.biomeTypes.isEmpty()
                || !condition.dimTypes.isEmpty() || !condition.biomeTags.isEmpty()
                || !condition.blocks.isEmpty() || !condition.structureTypes.isEmpty()
                || !condition.entityTypes.isEmpty() || !condition.bossTypes.isEmpty();

        // positives function as an OR, at least one of them must hold (or none specified)
        boolean positivesHold = !hasPositives
                || anyEventMatches(condition.songpackEvents)
                || anyBiomeMatches(condition.biomeTypes)
                || anyBiomeTagMatches(condition.biomeTags)
                || anyDimMatches(condition.dimTypes)
                || anyBlockMatches(condition.blocks)
                || anyStructureMatches(condition.structureTypes)
                || anyEntityMatches(condition.entityTypes)
                || anyBossMatches(condition.bossTypes);

        if (!positivesHold) return false;

        // negated ("!EVENT") entries, none of them may match
        return !(anyEventMatches(condition.negSongpackEvents)
                || anyBiomeMatches(condition.negBiomeTypes)
                || anyBiomeTagMatches(condition.negBiomeTags)
                || anyDimMatches(condition.negDimTypes)
                || anyBlockMatches(condition.negBlocks)
                || anyStructureMatches(condition.negStructureTypes)
                || anyEntityMatches(condition.negEntityTypes)
                || anyBossMatches(condition.negBossTypes));

    }


    static boolean anyEventMatches(List<SongpackEventType> events) {
        for (SongpackEventType songpackEvent : events) {
            if (songpackEventMap.containsKey(songpackEvent) && songpackEventMap.get(songpackEvent)) {
                return true;
            }
        }
        return false;
    }

    static boolean anyBiomeMatches(List<String> biomes) {
        for (var biome : biomes) {
            if (currentBiomeName.contains(biome)) {
                return true;
            }
        }
        return false;
    }

    static boolean anyBiomeTagMatches(List<TagKey<Biome>> biomeTags) {
        for (var biomeTag : biomeTags) {
            if (biomeTagEventMap.containsKey(biomeTag) && biomeTagEventMap.get(biomeTag)) {
                return true;
            }
        }
        return false;
    }

    static boolean anyDimMatches(List<String> dims) {
        for (var dim : dims) {
            if (currentDimName.contains(dim)) {
                return true;
            }
        }
        return false;
    }

    static boolean anyBlockMatches(List<RMEntryBlockCondition> blocks) {
        for (var blockCond : blocks) {
            for (var kvp : cachedBlockChecker.object2IntEntrySet()) {
                if (kvp.getKey().contains(blockCond.block) && kvp.getIntValue() >= blockCond.requiredCount) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean anyStructureMatches(List<String> structs) {
        for (var struct : structs) {
            for (var current : currentStructureNames) {
                if (current.contains(struct)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean anyEntityMatches(List<RMEntryEntityCondition> conds) {
        for (var cond : conds) {
            if (anySingleEntityConditionMatches(cond)) {
                return true;
            }
        }
        return false;
    }

    private static boolean anySingleEntityConditionMatches(RMEntryEntityCondition cond) {
        boolean matched = scanEntityCondition(cond);
        long now = entityScanCenterTick;

        if (matched) {
            // remember when it last held, the grace window below keys off this
            cond.lastMatchTick = now;
            return true;
        }

        // world time can jump backwards (leaving to the menu, changing worlds), drop the stale state
        if (cond.lastMatchTick > now) {
            cond.lastMatchTick = Long.MIN_VALUE;
            return false;
        }

        // Hysteresis: a mob walking across the below=/above= boundary would otherwise toggle the
        // condition every tick and make the music flap. Once the condition matched, keep it true for
        // ENTITY_CONDITION_GRACE_TICKS more, so crossing the edge has to be real and sustained.
        return cond.lastMatchTick != Long.MIN_VALUE
                && now - cond.lastMatchTick <= ENTITY_CONDITION_GRACE_TICKS;
    }

    private static boolean scanEntityCondition(RMEntryEntityCondition cond) {
        {
            int count = 0;
            int minCount = cond.minCount;
            // the scan itself only covers ENTITY_SCAN_RADIUS blocks, tighter bounds come from below=/above=
            double r2 = (double) ENTITY_SCAN_RADIUS * ENTITY_SCAN_RADIUS;
            double below2 = cond.belowDistance == null ? -1 : (double) cond.belowDistance * cond.belowDistance;
            double above2 = cond.aboveDistance == null ? -1 : (double) cond.aboveDistance * cond.aboveDistance;
            for (int i = 0; i < nearbyEntities.size(); i++) {
                if (!nearbyEntityIds.get(i).contains(cond.entity)) continue;
                var e = nearbyEntities.get(i);
                double dx = e.getX() - entityScanCenter.getX();
                double dy = e.getY() - entityScanCenter.getY();
                double dz = e.getZ() - entityScanCenter.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > r2) continue;
                // distance band, inclusive: below N = N or closer, above N = N or farther
                if (below2 >= 0 && distSq > below2) continue;
                if (above2 >= 0 && distSq < above2) continue;
                if (!entityTargetMatches(e, cond.targetMode)) continue;
                if (++count >= minCount) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks the mob's target against the requested mode.
     * <p>
     * Three clearly separated paths:
     * <ol>
     *   <li><b>Integrated server</b> (singleplayer / LAN host): the real mob target is read
     *       from the integrated server world directly, no server mod needed.</li>
     *   <li><b>Server mod packet</b> (dedicated server with ReactiveMusic-Server): the exact
     *       target arrives via {@link #onMobTargetPacket(UUID, UUID)} over the network.</li>
     *   <li><b>Pure-client heuristic</b> (dedicated server without server mod): contact damage,
     *       aggression flag, warden anger, and geometry.</li>
     * </ol>
     * <p>
     * {@code target=me} is true only when the server explicitly confirmed this exact player is
     * the mob's target. {@code target=other} is true only when the server explicitly confirmed
     * the mob targets someone/something else. If the server said the mob has no target, or no
     * information was received at all, both are false.
     */
    static boolean entityTargetMatches(Entity clientEntity, RMEntityTargetMode mode) {
        if (mode == RMEntityTargetMode.ANY) return true;

        MobTargetStatus status = resolveTargetStatus(clientEntity);
        if (status == null) {
            // нет точных данных (ни интегрированного сервера, ни серверного мода) — эвристика
            return heuristicTargetMatches(clientEntity, mode);
        }
        if (mode == RMEntityTargetMode.TARGET_ME) return status == MobTargetStatus.ME;
        return status == MobTargetStatus.OTHER;
    }

    /**
     * Статус цели моба для локального игрока: из интегрированного сервера (SP/LAN) или из пакетов
     * серверного мода (dedicated). {@code null} — точных данных нет, использовать эвристику.
     */
    private static MobTargetStatus resolveTargetStatus(Entity clientEntity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return null;

        if (mc.getServer() != null) {
            return serverSideTargetStatus(clientEntity);
        }
        if (clientHandshakeManager.isAcknowledged()) {
            return serverMobTargets.getOrDefault(clientEntity.getUuid(), MobTargetStatus.NONE);
        }
        return null;
    }

    /** Статус цели моба из интегрированного сервера; {@code null} если моб не найден на сервере. */
    private static MobTargetStatus serverSideTargetStatus(Entity clientEntity) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getServer() == null || mc.player == null) return null;

            var serverWorld = mc.getServer().getWorld(clientEntity.getEntityWorld().getRegistryKey());
            if (serverWorld == null) return null;

            var serverEntity = serverWorld.getEntityById(clientEntity.getId());
            if (!(serverEntity instanceof MobEntity mob)) return null;

            var target = getMobTarget(mob);
            if (target == null) return MobTargetStatus.NONE;
            return target.getUuid().equals(mc.player.getUuid())
                    ? MobTargetStatus.ME : MobTargetStatus.OTHER;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The mob's target. Since 1.21 most mobs run on the brain system and keep their target in
     * brain memory instead of the classic field, so both are checked.
     */
    private static net.minecraft.entity.LivingEntity getMobTarget(MobEntity mob) {
        var target = mob.getTarget();
        if (target != null) return target;

        try {
            Method getBrain = mob.getClass().getMethod("getBrain");
            Object brain = getBrain.invoke(mob);
            if (brain == null) return null;

            Class<?> memoryTypeClass = Class.forName("net.minecraft.entity.ai.brain.MemoryModuleType");
            Object attackTarget = memoryTypeClass.getField("ATTACK_TARGET").get(null);

            for (Method m : brain.getClass().getMethods()) {
                if (!m.getName().startsWith("get") || m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].getName().contains("MemoryModuleType")) continue;
                if (!java.util.Optional.class.isAssignableFrom(m.getReturnType())) continue;
                Object optional = m.invoke(brain, attackTarget);
                if (optional instanceof java.util.Optional<?> opt && opt.isPresent()
                        && opt.get() instanceof net.minecraft.entity.LivingEntity living) {
                    return living;
                }
            }
        } catch (Throwable ignored) {
            // older versions have no brain system
        }

        return null;
    }

    /**
     * Pure-client target heuristic. Runs when no integrated server exists (vanilla multiplayer,
     * dedicated server) and uses only state the server actually syncs to clients:
     * contact damage, the mob's aggression flag, warden anger, and geometry.
     */
    private static boolean heuristicTargetMatches(Entity clientEntity, RMEntityTargetMode mode) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;

        // A creative / spectator player is never a mob target in vanilla — never count as "me".
        if (mc.player.isCreative() || mc.player.isSpectator()) return false;

        boolean hitUsRecently = recentEntityDamageSources.containsKey(clientEntity);
        boolean attacking = clientEntity instanceof MobEntity mob && mob.isAttacking();
        boolean angryAtUs = isAngryAtPlayer(clientEntity);

        if (mode == RMEntityTargetMode.TARGET_ME) {
            if (hitUsRecently || angryAtUs) return true;

            // geometry: has to be an actual hunter, not just a mob that happens to face us
            return attacking
                    && looksAtPlayer(clientEntity)
                    && isApproachingPlayer(clientEntity)
                    && hasLineOfSight(clientEntity)
                    && isNearestPlayer(clientEntity);
        }

        // targeting something that is not us
        return attacking && !hitUsRecently && !angryAtUs && !looksAtPlayer(clientEntity);
    }

    /** Warden anger is one of the very few AI states vanilla syncs to clients (data tracker). */
    private static boolean isAngryAtPlayer(Entity entity) {
        if (entity instanceof WardenEntity warden) {
            return warden.getAnger() >= WARDEN_HUNTING_ANGER;
        }
        return false;
    }

    /** "Is this mob looking at the player" — head yaw leads the body when a mob turns. */
    private static boolean looksAtPlayer(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;

        double dx = mc.player.getX() - entity.getX();
        double dz = mc.player.getZ() - entity.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.001) return true;
        if (distance > TARGET_FACING_RANGE) return false;

        double yaw = Math.toRadians(entity.getHeadYaw());
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        double dot = (lookX * dx + lookZ * dz) / distance;

        return dot > TARGET_FACING_DOT;
    }

    /** Is the mob moving towards the player, not just standing or strafing. */
    private static boolean isApproachingPlayer(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;

        Vec3d velocity = entity.getVelocity();
        double dx = mc.player.getX() - entity.getX();
        double dz = mc.player.getZ() - entity.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.001) return true;

        return (velocity.x * dx + velocity.z * dz) / horizontal > 0.01;
    }

    /** Blocks between the mob's eyes and ours, so a mob behind a wall is not counted as hunting. */
    private static boolean hasLineOfSight(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return true;

        Vec3d from = new Vec3d(entity.getX(), entity.getEyeY(), entity.getZ());
        Vec3d to = new Vec3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ());

        BlockHitResult hit = mc.world.raycast(new RaycastContext(from, to,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity));

        return hit.getType() == HitResult.Type.MISS;
    }

    /** Another player closer to the mob means it is probably after them, not us. */
    private static boolean isNearestPlayer(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return false;

        double best = mc.player.squaredDistanceTo(entity);
        for (PlayerEntity other : mc.world.getPlayers()) {
            if (other == mc.player || other.isSpectator()) continue;
            if (other.squaredDistanceTo(entity) < best) return false;
        }
        return true;
    }

    static boolean anyBossMatches(List<RMEntryBossCondition> conds) {
        for (var cond : conds) {
            for (var current : currentBossNames) {
                if (!current.contains(cond.name)) continue;
                Float percent = currentBossPercents.get(current);
                if (percent == null) continue;
                float hp = percent * 100f;
                // inclusive bounds, same as the entity distance band
                if (cond.belowPercent != null && hp > cond.belowPercent) continue;
                if (cond.abovePercent != null && hp < cond.abovePercent) continue;
                return true;
            }
        }
        return false;
    }

    static boolean hasNearbyEntity(String... substrings) {
        for (var id : nearbyEntityIds) {
            for (var sub : substrings) {
                if (id.contains(sub)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasNearbyEntityIdEnding(String suffix) {
        for (var id : nearbyEntityIds) {
            if (id.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }









}
