# ReactiveMusic: Community Edition

> Community fork of [Reactive Music](https://github.com/CircuitLord/ReactiveMusic) by CircuitLord.
> If upstream development resumes in full, this fork will defer to it — finished features are offered upstream as PRs.

Reactive Music trades Minecraft's default music for something dynamic, reactive, and ever-changing. Including a collection of fantasy and celtic tracks to give an air of wonder, a whisper of the unknown, and the call of adventure.

## What this fork adds

**New triggers** (see `docs/MAKING_SONGPACKS.md` for the full reference):
- `STRUCT=` / `STRUCTURE=` — music inside structures (singleplayer)
- `ENTITY=type[,radius][,count]` — specific mobs, e.g. `ENTITY=pillager,48,3`
- `BOSS=name` — per-boss themes matched against the boss bar title
- `COMBAT` — real back-and-forth fights (hit trading + danger gating, no trigger on passive mobs or item frames)
- `BATTLE`, `WARDEN`, `WITHER`, `DRAGON` — outnumbered detector and boss mobs without bars
- `NETHER_FORTRESS`, `BASTION`, `END_CITY`, `OCEAN_TEMPLE`, `FARM`, `RAID` — structure tags from nearby mobs
- `!` negation for any event (`DAY` + `!RAIN`), `ignoreMusicDelay` and `alwaysOverride` entry flags

**Playback & formats:**
- `.ogg` (Vorbis) and `.wav` support alongside `.mp3`, no conversion needed
- MP3 validation at songpack load (renamed `.m4a` files are reported, not played as silence)
- `/reactivemusic pause|resume|skip|next` commands and a now-playing toast
- Dynamic FPS volume setting applies to our music too

**Packs & config:**
- Multiple songpacks at once (`activeSongpacks`), custom biome tags (`customBiomeTags`), configurable height thresholds
- Player thread survives playback errors, shared zip handling fixes

## Supported versions

- Fabric: `1.19.2`, `1.20.1`, `1.21.1`, `1.21.11`
- Forge: `1.19.2`, `1.20.1`
- NeoForge: `1.21.1`

Build all artifacts with `./gradlew chiseledBuild` (see `docs/MAKING_SONGPACKS.md` for songpack authoring).

## License

GPLv3, same as upstream. All credit for the original mod, music and idea goes to CircuitLord.

Discord (upstream): https://discord.gg/vJ6BCjPR3R
