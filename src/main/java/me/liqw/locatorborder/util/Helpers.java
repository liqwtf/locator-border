package me.liqw.locatorborder.util;

import net.minecraft.util.Mth;

public final class Helpers {
    public static float smoothstep(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
