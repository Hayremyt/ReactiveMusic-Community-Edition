package circuitlord.reactivemusic.compat;

import circuitlord.reactivemusic.platform.PlatformHelper;
import net.minecraft.sound.SoundCategory;

import java.lang.reflect.Method;

public class CompatUtils {
    public static boolean isModLoaded(String id) {
        return PlatformHelper.INSTANCE.isModLoaded(id);
    }

    public static boolean isClothConfigLoaded() {
        return isModLoaded("cloth-config2");
    }

    private static boolean dynamicFpsChecked = false;
    private static Method dynamicFpsVolumeMethod = null;

    /**
     * Mirrors Dynamic FPS's per-state volume multiplier. Dynamic FPS works by mixing into
     * vanilla's SoundEngine, which Reactive Music bypasses with its own audio line, so without
     * this its unfocused-volume setting would never affect our music.
     * Returns 1.0 when Dynamic FPS is absent or its API can't be reached.
     */
    public static float getDynamicFpsMusicMultiplier() {
        if (!dynamicFpsChecked) {
            dynamicFpsChecked = true;
            if (isModLoaded("dynamicfps")) {
                try {
                    Class<?> clazz = Class.forName("dynamic_fps.impl.DynamicFPSMod");
                    dynamicFpsVolumeMethod = clazz.getMethod("volumeMultiplier", SoundCategory.class);
                } catch (Exception ignored) {
                    dynamicFpsVolumeMethod = null;
                }
            }
        }

        if (dynamicFpsVolumeMethod == null) return 1.0f;

        try {
            float master = (Float) dynamicFpsVolumeMethod.invoke(null, SoundCategory.MASTER);
            float music = (Float) dynamicFpsVolumeMethod.invoke(null, SoundCategory.MUSIC);
            return Math.min(1.0f, Math.max(0.0f, master * music));
        } catch (Exception e) {
            dynamicFpsVolumeMethod = null;
            return 1.0f;
        }
    }
}
