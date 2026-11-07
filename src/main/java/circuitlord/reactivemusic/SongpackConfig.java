package circuitlord.reactivemusic;

import circuitlord.reactivemusic.config.MusicDelayLength;
import circuitlord.reactivemusic.config.MusicSwitchSpeed;

import java.nio.file.Path;

public class SongpackConfig {

    public String name;
    public String version = "";
    public String author = "";
    public String description = "";

    public String credits = "";

    public MusicDelayLength musicDelayLength = MusicDelayLength.NORMAL;

    public MusicSwitchSpeed musicSwitchSpeed = MusicSwitchSpeed.NORMAL;

    // fade-out length in ticks (20 per second) used by entries with `customFade: true`; 0 = instant cut
    public int customFadeTicks = 0;

    public SongpackEntry[] entries;


}


