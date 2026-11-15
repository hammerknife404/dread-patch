package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.entity.DreadQueen;

/**
 * Ice and Fire's dread spawn animation (its {@code DreadBaseModel.animate}) as plain numbers. With
 * Uranus' keyframes it starts 35 model pixels underground with both arms straight up, rises over
 * 30 ticks easing out ({@code sin(t·π/2)}, as Uranus interpolates), then lowers the arms over 5.
 * {@code tick} is the animation tick plus the partial tick.
 */
final class RisePose {
    /** Model pixels below her normal position at tick 0: her full height and her headdress. */
    static final float START_DEPTH = 35.0F;
    static final int RISE_TICKS = DreadQueen.RISE_TICKS;
    static final int LOWER_TICKS = 5;

    private RisePose() {
    }

    /** How far below her normal position she is, in model pixels (down is positive). */
    static float depth(float tick) {
        if (tick >= RISE_TICKS) {
            return 0.0F;
        }
        return START_DEPTH * (1.0F - ease(Math.max(tick, 0.0F) / RISE_TICKS));
    }

    /** How much of the arms-overhead pose (and the waving with it) is on, from 1 to 0. */
    static float armsUp(float tick) {
        if (tick < RISE_TICKS) {
            return 1.0F;
        }
        if (tick >= RISE_TICKS + LOWER_TICKS) {
            return 0.0F;
        }
        return 1.0F - ease((tick - RISE_TICKS) / LOWER_TICKS);
    }

    private static float ease(float t) {
        return (float) Math.sin(t * Math.PI / 2.0);
    }
}
