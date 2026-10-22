
# Making Songpacks

Songpacks are constructed as folders with a yaml configuration file and a folder with mp3 music files.

They're loaded from the `resourcepacks` folder, although they're not actually resource packs and are instead selected from the configuration UI. This is purely to make including songpacks easier.

**Download the Songpack Template [here](https://raw.githubusercontent.com/CircuitLord/ReactiveMusic/master/docs/ReactiveMusicSongpackTemplate-v6.zip)!**

You can find a video version of this wiki page [here](https://www.youtube.com/watch?v=YvEap8IUS-c)!
> Highly recommended to use VSCode with the YAML extension as shown in the video, or else you will have issues making properly formatted YAML files!


## Testing Songpacks

Open the songpack menu using `/reactivemusic`. Click on the button for your songpack name and it'll start playing.
This also works for reloading a songpack if you've made changes you want to try without restarting your game by clicking another songpack and back onto yours.

I also highly recommend turning debug mode on in the Debug section,
this will make songs switch whenever their events become valid and removes silence gaps entirely to make it easier to test.

You can also run `/reactivemusic toggleLogging` to display what the mod is doing in real-time.

Music controls: `/reactivemusic pause`, `/reactivemusic resume`, `/reactivemusic skip` (or `next`). A now-playing toast shows on every song switch, toggle it with `showNowPlayingToast` in `config/ReactiveMusic.json5`.

Music files must be real MP3s — renamed `.m4a`/`.ogg`/`.wav` files are rejected at load with an error in the songpack details instead of playing silence. Missing files are reported the same way, including the folder that was searched.

<br>

## Songpack Configuration

`ReactiveMusic.yaml` is where all the configuration for your songpack takes place. Let's go over all the properties available in a songpack config.

### Global configuration:
```
name: "My Awesome Songpack"
version: "1.0"
author: "CircuitLord"
description: "A really good songpack that I made"
credits: "Cool people here"

musicSwitchSpeed: NORMAL
musicDelayLength: NORMAL
```

- `(String) name` The unique name/identifier for your songpack.
-  `musicSwitchSpeed` (INSTANT, SHORT, NORMAL, LONG) Defines the default for how fast music will stop when it's event becomes invalid
-  `musicDelayLength` (NONE, SHORT, NORMAL, LONG) Defines the default for how much silence there should be before a new song starts playing again

<br><br>

## Songpack Entries:

Songpack entries define what songs play in specific events. You can have as many entries as you want, and they're prioritized internally based on what order you put them in.

Here's an example songpack entry:

```
- events: [ "MAIN_MENU" ]
  songs:
    - "DuNock-Street-TitleEdit"
```

- `(SongpackEventType) events` The specific events that need to be valid for this entry to play. (see details below)
- `(String[]) songs` The list of song files to pick from when this event plays. These are picked from the `music` sub-folder. Names can be bare (`"Freedom"` prefers `.mp3`, old packs keep working as-is) or carry an explicit extension (`"Intro.ogg"`, `"Outro.wav"`). Supported formats: `.mp3`, `.ogg` (Vorbis), `.wav` (surround files are downmixed to stereo). Anything else (renamed `.m4a`, etc.) is rejected at load with an error instead of playing silence.

<br><br>

### Entry flags

#### `allowFallback` (Boolean, default false)
If we've played all songs from this entry recently, borrow fresh ones from lower valid entries instead of repeating. Good for one-off events you don't want on loop.
```
  - events: [ "HOME", "SNOW", "NIGHT" ]
    allowFallback: true
    songs:
      - "WintersNight"
```

#### `forceStopMusicOnChanged` (Boolean, default false)
Stop the current music when this entry becomes valid OR invalid (sets both directions below). Good when the music must switch no matter what (boss, nether).

#### `forceStopMusicOnValid` (Boolean, default false)
Stop the current music only when this entry becomes valid.

#### `forceStopMusicOnInvalid` (Boolean, default false)
Stop the current music only when this entry becomes invalid. The music then falls back down the list.

#### `forceStartMusicOnValid` (Boolean, default false)
When this entry becomes valid (and music stopped naturally or by a forceStop), start it immediately instead of waiting out the silence delay.
```
  - events: [ "BOSS" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    songs:
      - "LastStand"
```

#### `forceChance` (float, default 1.0)
Probability (0.0–1.0) for the force actions above. Rolled once per change, all actions of the entry share the roll. Good for events that should only sometimes interrupt.
```
  - events: [ "DIM=OVERWORLD", "SUNSET" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    forceChance: 0.5
    songs:
      - "Sombre"
```

#### `ignoreMusicDelay` (Boolean, default false)
Skip the `musicDelayLength` silence before starting this entry. Good for BOSS/CREDITS entries when you use a LONG delay everywhere else.

#### `alwaysOverride` (Boolean, default false)
If this entry is valid it wins over every entry without the flag, no matter where it sits in the file. Good for BOSS (boss interrupts home music). Combine with `forceStopMusicOnChanged` for an instant switch.

<br><br>


### IMPORTANT: Spacing matters! Use tabs to properly indent the entries and their properties.
A proper entry looks like this!
```
(tab) - events: [ "DAY" ]
(tab)(tab)alwaysPlay: true
(tab)(tab)songs:
(tab)(tab)(tab) - "MyAwesomeSong"
(tab)(tab)(tab) - "MyOtherCoolSong"
```

<br><br>

You can also combine multiple events for more specific songs, or do an "OR" condition

```
  - events: [ "DAY", "BIOME=MOUNTAIN" ]
    songs:
      - "ForTheKing"
      - "Freedom"

  - events: [ "BIOME=ocean || UNDERWATER" ]
    songs:
      - "Freedom"
```

You can also negate any event with `!`:

```
  - events: [ "DAY", "!RAIN" ]
    songs:
      - "ForTheKing"

  - events: [ "!BIOME=ocean", "!STRUCT=village" ]
    songs:
      - "Freedom"
```

## Conditions cheat-sheet

Two kinds of things go into `events`, nothing else:

- **Bare dictionary words** — fixed detectors, written as-is: `DAY`, `BOSS`, `RAID`, `NEARBY_MOBS`, `WARDEN`...
- **Prefixed conditions** — for concrete objects: `BIOME=x`, `BIOMETAG=x`, `DIM=x`, `STRUCT=x`, `ENTITY=x`, `BLOCK=name,count`, `BOSS=name`

Bare mob/structure names alone are errors, not matches:
```
  - events: [ "NEARBY_MOBS" ]     # right: any hostiles nearby
  - events: [ "ENTITY=zombie" ]   # right: specific zombies
  - events: [ "zombie" ]          # WRONG: unknown event, entry gets skipped with an error
  - events: [ "STRUCT=village" ]  # right: village structure
```

## Entry priority (order matters!)

Every tick the mod collects all valid entries **in file order** and plays the first one. A broad entry placed high shadows everything below it while it stays valid. Exception: entries with `alwaysOverride: true` jump ahead of everything without the flag (first among themselves, still by file order).

Order from specific to generic. Recommended chain for fights:

```
  - events: [ "BOSS=wither" ]   # exact boss theme
  - events: [ "BOSS" ]          # any boss bar
  - events: [ "WARDEN" ]        # boss mob without a bar
  - events: [ "BATTLE" ]        # outnumbered swarm
  - events: [ "COMBAT" ]        # back-and-forth fight
  - events: [ "NEARBY_MOBS" ]   # tense presence
  - events: [ "DAY" ]           # fallback
```

Two more tricks: a higher entry with `forceStopMusicOnChanged` interrupts immediately when it becomes valid (boss spawns mid-fight); falling back down follows the normal silence delay unless the lower entry has `ignoreMusicDelay`. And if two entries share the same song file, the handoff is seamless (no restart).

<br><br>

## Events (SongpackEventTypes)

All available event types. Remember: within one entry, different events mean AND; `||` inside one string means OR; `!` negates; the first valid entry in file order wins.

### Special
- `MAIN_MENU` (no world loaded — title screen)
- `CREDITS` (credits screen open)
- `HOME` (within 45 blocks of where you last slept; the spot saves automatically on sleep)

### Time (day is 0–24000 ticks; overlaps happen — SUNSET/SUNRISE are also DAY, order decides)
- `DAY` (everything except 13000–23000)
- `NIGHT` (13000–23000)
- `SUNSET` (12000–13000, always together with DAY)
- `SUNRISE` (23000–24000, always together with DAY)

### Weather (rain vs snow depends on the biome you're standing in)
- `RAIN` (it's raining and your biome rains)
- `SNOW` (it's raining and your biome snows)
- `STORM` (thunderstorm, anywhere)

### World Height (thresholds configurable, see Height event thresholds below)
- `UNDERWATER` (head under water)
- `UNDERGROUND` (overworld, no sky above you, below `undergroundMaxY`)
- `DEEP_UNDERGROUND` (same, below `deepUndergroundMaxY` — always fires together with UNDERGROUND, so put it higher in the file)
- `HIGH_UP` (overworld, sky visible, above `highUpMinY`)

### Entities
- `NEARBY_MOBS` (1 or more hostiles within 12 blocks sideways / 6 up-down — presence detector, use COMBAT for real fights)
- `MINECART` (riding a minecart)
- `BOAT` (riding a boat, chest boats count)
- `HORSE` (riding a horse)
- `PIG` (riding a saddled pig)

### Actions
- `FISHING` (fishing rod cast out)
- `DYING` (health below 35% of max)

### Location
- `VILLAGE` (1 or more villagers within 30 blocks sideways / 15 up-down)

### Special values
- `GENERIC` (always true — the ultimate fallback, put it last if nothing else should leave silence)
- `OVERWORLD` / `NETHER` / `END` (legacy dimension checks, prefer `DIM=` which also covers modded dimensions)
- `NONE` (never matches anything, technical placeholder)

### Structure tags (from nearby mobs, all within the shared 64-block entity scan)
- `NETHER_FORTRESS` (wither skeletons or blazes nearby)
- `BASTION` (piglin brutes nearby — brutes only live in bastions, unlike regular piglins)
- `END_CITY` (shulkers nearby)
- `OCEAN_TEMPLE` (guardians nearby AND you are underwater)
- `FARM` (cows, chickens, sheep and pigs all nearby at once — keep it low priority, everything overwrites it. Note: piglins also contain "pig", so bastions can trigger it too)
- `RAID` (illagers nearby: pillagers, vindicators, evokers, ravagers, illusioners)

```
  - events: [ "NETHER_FORTRESS" ]
    forceStopMusicOnChanged: true
    songs:
      - "Mufaya-DarkAscension"
```

### Combat
- `BOSS` (whenever a boss bar is on-screen)
- `BOSS=name` (boss bar title contains `name`, e.g. `BOSS=wither`, `BOSS=dragon` — case-insensitive, spaces don't matter, any HP)
- `BOSS=name,below=<percent>` / `BOSS=name,above=<percent>` (same, but only while the bar is below/above that HP percent — phase themes, e.g. `BOSS=wither,below=50`). Any number 0–100, decimals allowed (`below=12.5`). **Bounds are inclusive** (`below=50` matches a bar sitting at exactly 50%), same as the entity distance band. Combine both for a band (`below=50,above=20` = between 20% and 50%). With several same-named bars the lowest HP counts. Numbers outside 0–100 parse but are meaningless (`below=150` is always true).

```
  # wither second phase theme, takes over when the bar drops below half
  - events: [ "BOSS=wither,below=50" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    songs:
      - "Mufaya-DarkAscension"
```
- `WARDEN`, `WITHER`, `DRAGON` (that boss mob nearby, no boss bar needed — WITHER matches the boss exactly, not skeletons)
- `BATTLE` (5 or more distinct recent attackers within the last 10 seconds — a rough "outnumbered" detector)
- `COMBAT` (an actual back-and-forth fight: 3+ hits traded in the last 10 seconds plus danger — recently hurt or hostile mobs nearby, so poking one zombie doesn't count. A single hit over 25% of your max HP counts on its own. Disabled in creative/spectator)

```
  - events: [ "COMBAT" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    songs:
      - "LastStand"
      - "Ruthless"
```

```
  # wither gets its own theme, other bosses fall through to generic BOSS
  - events: [ "BOSS=wither" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    songs:
      - "Mufaya-DarkAscension"

  - events: [ "BOSS" ]
    forceStopMusicOnChanged: true
    forceStartMusicOnValid: true
    songs:
      - "LastStand"
      - "Ruthless"
```

<br><br>

## Biome Events

You can search for any biome by using `BIOME=biomename`. This can be the full biome id or just a part of it (case-insensitive substring, so modded biomes work the same way).

```
  # any biome with cherry in the name
  - events: [ "BIOME=cherry" ]
    songs:
      - "Storm"
```

<br><br>

Biome tags can be used by specifying `BIOMETAG=BIOME_TAG`. They're good for automatically having compatibility with modded biomes. You can find the full list of tags [here](https://maven.fabricmc.net/docs/fabric-api-0.100.3+1.21/net/fabricmc/fabric/api/tag/convention/v2/ConventionalBiomeTags.html)

NOTE: putting the "IS_" in front of the tag is optional ("BIOMETAG=IS_MOUNTAIN" and "BIOMETAG=MOUNTAIN" are both valid

```
  - events: [ "DAY", "BIOMETAG=IS_HOT" ]
    songs:
      - "ForTheKing"
```

<br><br>


## Dimension Events

Dimensions can be specified by doing `DIM=dimname`. Similar to biome events, this can be the fully typed name or just a subset of it.

```
  # we add forceStopMusicOnChanged here so that the song always changes upon going in/out of the nether, since that music is pretty different than anything else
  - events: [ "DIM=NETHER" ]
    forceStopMusicOnChanged: true
    songs:
      - "Storm"
```

<br><br>


## Structure Events

Structures can be specified by doing `STRUCT=structname` (`STRUCTURE=` works too as an alias). Similar to biome/dimension events, this can be the fully typed id or just a subset of it (case-insensitive). You match by being inside the structure's bounding box (checked over the 3×3 chunks around you). Modded structures work the same way, use their namespace id.

```
  # any village variant
  - events: [ "STRUCT=village" ]
    songs:
      - "KeeperOfTheForest"

  # exact structure + OR condition
  - events: [ "STRUCT=minecraft:ancient_city || STRUCT=trial_chambers" ]
    forceStopMusicOnChanged: true
    songs:
      - "Storm"
```

Find structure ids with the vanilla command `/locate structure minecraft:` (autocomplete lists everything, including modded ones).

NOTE: structure data is never sent to the client by vanilla, the mod reads it from the integrated singleplayer server. `STRUCT` events only work in singleplayer (or LAN with cheats/host), on dedicated servers they never match.

<br><br>


## Entity Events

Entities can be specified by doing `ENTITY=typename`. The type is matched as a substring of the entity id, so modded mobs work too.

Optional parameters are named, so the order doesn't matter and you only write what you need:

| Parameter | Meaning | Default |
|---|---|---|
| `c=<count>` | how many matching mobs are needed | 1 |
| `below=<blocks>` | only mobs **closer** than this | unbounded |
| `above=<blocks>` | only mobs **farther** than this | unbounded |
| `target=me` / `target=other` | who the mob is targeting | any |

`below`/`above` are **distances in blocks** (unlike `BOSS=`, where the same words mean HP percent — the meaning follows what is being measured). **Both ends are inclusive**: `below=40` still matches a mob standing exactly 40 blocks away, `above=20` still matches one exactly 20 blocks away, so `above=20,below=40` covers the whole 20–40 band including its edges. The scan itself covers 64 blocks, so bounds beyond that see nothing.

```
  # raid music when 3+ pillagers within 48 blocks
  - events: [ "ENTITY=pillager,below=48,c=3" ]
    forceStopMusicOnChanged: true
    songs:
      - "LastStand"

  # a horde closing in: 4+ zombies between 20 and 40 blocks
  - events: [ "ENTITY=zombie,above=20,below=40,c=4" ]
    forceStopMusicOnChanged: true
    songs:
      - "LastStand"

  # ender dragon nearby (3D distance)
  - events: [ "ENTITY=ender_dragon,below=64" ]
    songs:
      - "Ruthless"

  # only when the zombies are actually after you
  - events: [ "ENTITY=zombie,target=me" ]
    songs:
      - "Ruthless"
```

Positional values are not accepted here (`ENTITY=zombie,32` is an error) — this condition is new, so there is nothing to stay compatible with.

`target=me` / `target=other` check what the mob is aiming at. Vanilla never sends mob targets to the client, so this is read from the integrated server and is exact in singleplayer (and on a LAN host). On a dedicated server the mod has to guess: a mob that recently hit you counts as targeting you, a mob that is fighting something else counts as targeting someone else — so treat it as a hint there, not a fact.

Find entity ids with the vanilla command `/summon ` (autocomplete lists everything, including modded ones).

<br><br>


## Block Events

In version 1.2.0 and above, you can detect nearby blocks in a 51×51×51 cube around you (25 blocks each way, rescanned continuously).

Use `/reactivemusic logBlockCounter` to print the nearby block counts to get an idea of what numbers to use.

Fortress detection example:
```
# 1000 nether bricks and 2 brick fences within 25 blocks
[ "DIM=NETHER", "BLOCK=nether_bricks,c=1000", "BLOCK=nether_brick_fence,c=2" ]
```

The plain form `BLOCK=nether_bricks,1000` still works, `c=` is just the explicit version.


<br><br>


# Mod Configuration

## Height event thresholds

In `config/ReactiveMusic.json5` you can change the Y levels for height-based events (defaults match vanilla overworld generation). Useful on modded terrain (Terralith, Tectonic, Lithosphere) or servers with changed world height where the defaults never trigger.

```
	undergroundMaxY: 55,
	deepUndergroundMaxY: 15,
	highUpMinY: 128,
```

- `undergroundMaxY` — `UNDERGROUND` triggers below this Y (without sky access)
- `deepUndergroundMaxY` — `DEEP_UNDERGROUND` triggers below this Y (without sky access)
- `highUpMinY` — `HIGH_UP` triggers above this Y

## Making Sounds Mute Music

In-game, do `/reactivemusic toggleSoundEventLogging` to start printing the nearby sound events happening into chat.

In the ReactiveMusic.json5 config, you can edit the `soundsMuteMusic` field with the names/partial names of any sound events that you want to mute RM when they play. This respects positional sound distance. This requires a game restart to be re-loaded.

If a mod incorrectly tags non-positional music as positional, use `soundsMuteMusicIgnoreDistance` instead.

Example:
```
  # Mute RM when near a beacon block
	soundsMuteMusic: [
		"block.beacon.ambient",
		"othermod.xy"
	],

  # Mute RM whenever this sound plays, even if the sound position is wrong
	soundsMuteMusicIgnoreDistance: [
		"boss.theme",
		"partial-event-name"
	],
```

## Block counter blacklist

`BLOCK=` conditions count blocks around you. If some blocks are noise for your songpack (ores, debris), exclude them by id substring in `config/ReactiveMusic.json5`:

```
	blockCounterBlacklist: [
		"ore",
		"debris"
	],
```

Empty by default, so every block is counted.

## Dynamic FPS compatibility

If the Dynamic FPS mod is installed, its volume setting (e.g. quieter when unfocused) applies to Reactive Music too — the mod mirrors Dynamic FPS's multiplier onto its own audio line, since Dynamic FPS can only see vanilla sounds.

## Custom biome tags
In `config/ReactiveMusic.json5` you can list extra biome tags usable as `BIOMETAG=` in any songpack. Good for modded tags (Nature's Spirit `is_alpine`) or vanilla tags the mod doesn't use by default (`has_structure/village`).

```
	customBiomeTags: [
		"is_alpine",
		"has_structure/village"
	],
```

## Multiple songpacks at once

In `config/ReactiveMusic.json5` you can list several songpacks to play at the same time. Entries are merged in listed order (earlier packs win ties), each entry keeps its own pack's music files and switch/delay defaults. Empty list (default) keeps the classic single-pack behavior from the in-game menu — clicking Load there exits multi-pack mode.

```
	activeSongpacks: [
		"Adventure Redefined",
		"My Awesome Songpack"
	],
```

Requires a game restart to apply.

<br><br>

