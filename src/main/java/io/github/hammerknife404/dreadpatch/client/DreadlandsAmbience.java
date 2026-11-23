package io.github.hammerknife404.dreadpatch.client;

import io.github.hammerknife404.dreadpatch.magic.AmbienceRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The Dreadlands' ambience (0.54.0, the owner's second drop: "a little louder, with surface ice cracking
 * and deeper underground rumbles, each sound's volume varying a little"), in place of the biome's own loop
 * and additions (its cave mood is still the biome's):
 * <ul>
 * <li>its loop, the soul sand valley's at full volume (vanilla plays it at 0.85), drifting slowly between
 * 0.88 and 1 like the wind rising and falling, fading in and out as you come and go;</li>
 * <li>its whispers and shifting sand, vanilla's, a fifth louder, as often as vanilla's (0.0111 a tick);</li>
 * <li>ice cracking on the surface round you, 6-20 blocks off, every 30-120 s while you're near the top, half
 * the time a burst of two or three back to back ({@link AmbienceRules}; 0.54.1);</li>
 * <li>a deep rumble from below about every 30 s, louder when you're underground.</li>
 * </ul>
 * Every one plays at its own volume, ±15%.
 */
public final class DreadlandsAmbience {
    /** Half vanilla's (0.58.0, the owner: "Reduce the ambient soul sounds some and their frequency"). */
    private static final float ADDITIONS_CHANCE = 0.0055F;
    /** Syndrigossa, far off (0.58.0): every 3-8 minutes, from somewhere new each time. */
    private static final int ROAR_MIN = 3600;
    private static final int ROAR_MAX = 9600;
    private static final double ROAR_DISTANCE = 64.0;
    private static int roarIn = -1;
    private static final int RUMBLE_ONE_IN = 600;
    /** Within this far under the ground's top over you, you hear the ice. */
    private static final int NEAR_SURFACE = 6;
    /** Further than this under it, the rumbles are louder. */
    private static final int UNDERGROUND = 8;
    private static final float VARY = 0.15F;

    @Nullable
    private static Loop loop;
    /** Ticks to the next crack (or burst); -1 until it's set, as you come. */
    private static int crackIn = -1;
    /** Cracks still to come in this burst, the ticks to the next, and where the burst is. */
    private static int burstLeft;
    private static int burstGap;
    private static int burstX;
    private static int burstZ;

    private DreadlandsAmbience() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(DreadlandsAmbience::onClientTick);
    }

    private static boolean here(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.level.dimension() == Dreadlands.LEVEL;
    }

    /** {@code base}, ±15%. */
    static float vary(RandomSource random, float base) {
        return base * (1.0F - VARY + random.nextFloat() * 2.0F * VARY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) {
            return;
        }
        if (!here(minecraft)) {
            crackIn = -1;
            burstLeft = 0;
            roarIn = -1;
            return;
        }
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (loop == null || loop.isStopped() || !minecraft.getSoundManager().isActive(loop)) {
            loop = new Loop();
            minecraft.getSoundManager().play(loop);
        }
        RandomSource random = level.random;
        if (random.nextFloat() < ADDITIONS_CHANCE) {
            everywhere(minecraft, ModRegistries.DREADLANDS_ADDITIONS.get(), vary(random, 1.0F), 0.95F + random.nextFloat() * 0.1F);
        }
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ());
        if (crackIn < 0) {
            crackIn = AmbienceRules.crackDelay(random::nextInt);
        }
        if (burstLeft > 0) {
            if (--burstGap <= 0) {
                int spread = AmbienceRules.BURST_SPREAD;
                crack(level, random, burstX + random.nextInt(2 * spread + 1) - spread, burstZ + random.nextInt(2 * spread + 1) - spread);
                burstLeft--;
                burstGap = AmbienceRules.burstGap(random::nextInt);
            }
        } else if (--crackIn <= 0) {
            crackIn = AmbienceRules.crackDelay(random::nextInt);
            if (player.getY() >= top - NEAR_SURFACE) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = 6.0 + random.nextDouble() * 14.0;
                burstX = Mth.floor(player.getX() + Math.cos(angle) * distance);
                burstZ = Mth.floor(player.getZ() + Math.sin(angle) * distance);
                crack(level, random, burstX, burstZ);
                burstLeft = AmbienceRules.burstSize(random::nextInt) - 1;
                burstGap = AmbienceRules.burstGap(random::nextInt);
            }
        }
        if (roarIn < 0) {
            roarIn = ROAR_MIN + random.nextInt(ROAR_MAX - ROAR_MIN + 1);
        }
        if (--roarIn <= 0) {
            roarIn = ROAR_MIN + random.nextInt(ROAR_MAX - ROAR_MIN + 1);
            roar(minecraft, random);
        }
        if (random.nextInt(RUMBLE_ONE_IN) == 0) {
            float base = player.getEyeY() < top - UNDERGROUND ? 0.8F : 0.55F;
            everywhere(minecraft, ModRegistries.DREADLANDS_RUMBLE.get(), vary(random, base), 0.9F + random.nextFloat() * 0.2F);
        }
    }

    /** Ice cracking on the ground's top at {@code x}, {@code z}. */
    private static void crack(ClientLevel level, RandomSource random, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        level.playLocalSound(x + 0.5, y, z + 0.5, ModRegistries.DREADLANDS_ICE_CRACK.get(), SoundSource.AMBIENT,
                vary(random, 0.9F), 0.9F + random.nextFloat() * 0.25F, false);
    }

    /**
     * Syndrigossa's roar, far off: from a point 64 blocks away in a random direction, high up, unattenuated
     * (so it's heard) but quiet and pitched low, so it carries direction and sounds distant.
     */
    private static void roar(Minecraft minecraft, RandomSource random) {
        LocalPlayer player = minecraft.player;
        double angle = random.nextDouble() * Math.PI * 2.0;
        double x = player.getX() + Math.cos(angle) * ROAR_DISTANCE;
        double z = player.getZ() + Math.sin(angle) * ROAR_DISTANCE;
        minecraft.getSoundManager().play(new SimpleSoundInstance(ModRegistries.DREADLANDS_DISTANT_ROAR.get().getLocation(), SoundSource.AMBIENT,
                vary(random, 0.3F), 0.65F + random.nextFloat() * 0.1F, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, x, player.getY() + 24.0, z, false));
    }

    /** A sound heard all round, not from anywhere in particular. */
    private static void everywhere(Minecraft minecraft, SoundEvent sound, float volume, float pitch) {
        minecraft.getSoundManager().play(new SimpleSoundInstance(sound.getLocation(), SoundSource.AMBIENT, volume, pitch,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }

    /** Its loop, drifting, fading in as you come and out as you go. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private static final float FADE = 1.0F / 40.0F;
        private float fade;
        private float drift = 0.86F;
        private float driftTo = 0.86F;
        private int nextDrift;

        Loop() {
            super(ModRegistries.DREADLANDS_LOOP.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
        }

        @Override
        public void tick() {
            boolean here = here(Minecraft.getInstance());
            this.fade = Mth.clamp(this.fade + (here ? FADE : -FADE), 0.0F, 1.0F);
            if (!here && this.fade <= 0.0F) {
                this.stop();
                return;
            }
            if (--this.nextDrift <= 0) {
                this.driftTo = 0.80F + this.random.nextFloat() * 0.12F;
                this.nextDrift = 60 + this.random.nextInt(80);
            }
            this.drift += (this.driftTo - this.drift) * 0.02F;
            this.volume = Math.max(0.01F, this.fade * this.drift);
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}
