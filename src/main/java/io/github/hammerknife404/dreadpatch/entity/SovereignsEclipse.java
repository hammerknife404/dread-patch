package io.github.hammerknife404.dreadpatch.entity;

import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Sovereign's Eclipse, Syndrigossa's Rimefire Crown: a ring of dark spires and indigo dread fire
 * {@link #RADIUS} blocks round (16 across, against the crown's 6), standing {@link #LIFETIME} ticks
 * (twice the queen's crown). It doesn't close in, and it doesn't root: everyone inside it may harm is
 * heavily slowed (Slowness IV, a 60% cut) and kept under Withering Rime. It drinks the fire inside it
 * as the crown does. A Withering Rime-Bolt doesn't shatter it.
 */
public class SovereignsEclipse extends RimefireCrown {
    public static final float RADIUS = 8.0F;
    public static final double HEIGHT = 4.0;
    public static final int LIFETIME = 200;
    /** Slowness IV: 15% a level. */
    static final int SLOW_AMPLIFIER = 3;
    /** Withering Rime kept at least this long while they're inside. */
    static final int RIME_TICKS = 40;

    /** Abyssal indigo, and soot. */
    private static final Vector3f INDIGO_SMOKE = new Vector3f(0.18F, 0.16F, 0.45F);
    private static final Vector3f SOOT_SMOKE = new Vector3f(0.02F, 0.024F, 0.035F);

    public SovereignsEclipse(EntityType<? extends SovereignsEclipse> type, Level level) {
        super(type, level);
        this.setLifetime(LIFETIME);
    }

    public static SovereignsEclipse create(Level level, @Nullable LivingEntity owner, Vec3 at) {
        SovereignsEclipse eclipse = new SovereignsEclipse(ModRegistries.SOVEREIGNS_ECLIPSE.get(), level);
        eclipse.setPos(at);
        eclipse.setOwner(owner);
        return eclipse;
    }

    @Override
    public float radius() {
        return RADIUS;
    }

    @Override
    public double height() {
        return HEIGHT;
    }

    @Override
    protected void hold(LivingEntity victim, @Nullable Entity owner) {
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, SLOW_AMPLIFIER, false, false, true), owner);
        victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, RIME_TICKS), owner);
        victim.clearFire();
    }

    @Override
    protected Vector3f[] smoke() {
        return new Vector3f[]{INDIGO_SMOKE, SOOT_SMOKE};
    }
}
