package io.github.hammerknife404.dreadpatch.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Syndrigossa's breath, big enough to see from the ground: vanilla's soul fire flame and campfire
 * smoke sprites, starting several times vanilla's flame size and swelling as they go, flying exactly
 * where they're sent (vanilla's add their own jitter and dust slows to a tenth), slowing a little,
 * and fading over their last quarter. The flame burns full-bright from white-cyan to her dread-fire cyan; the smoke is
 * soot, rising a little.
 */
final class RimeBreathParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean smoke;
    private final float startSize;
    private final float growth;

    private RimeBreathParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, boolean smoke) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.smoke = smoke;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        // Slow to lose speed and long-lived, so one sent from her mouth carries the length of her
        // breath (DragonBreath.PARTICLE_TRAVEL blocks for each block a tick it's sent at).
        this.friction = smoke ? 0.95F : 0.96F;
        this.gravity = smoke ? -0.005F : 0.0F;
        this.lifetime = smoke ? 28 + this.random.nextInt(13) : 22 + this.random.nextInt(9);
        this.startSize = smoke ? 0.55F + this.random.nextFloat() * 0.2F : 0.4F + this.random.nextFloat() * 0.15F;
        this.growth = smoke ? 3.5F : 3.0F;
        this.quadSize = this.startSize;
        this.roll = this.random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        if (smoke) {
            float shade = 0.07F + this.random.nextFloat() * 0.05F;
            this.setColor(shade, shade * 1.1F, shade * 1.45F);
            this.alpha = 0.85F;
        }
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = (float) this.age / this.lifetime;
        this.quadSize = this.startSize * (1.0F + (this.growth - 1.0F) * life);
        if (!this.smoke) {
            // White-cyan at the mouth to her dread-fire cyan.
            this.setColor(Mth.lerp(life, 0.85F, 0.1F), Mth.lerp(life, 1.0F, 0.75F), 1.0F);
        }
        float fade = Mth.clamp((1.0F - life) * 4.0F, 0.0F, 1.0F);
        this.alpha = (this.smoke ? 0.85F : 1.0F) * fade;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public int getLightColor(float partialTick) {
        return this.smoke ? super.getLightColor(partialTick) : 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    static ParticleProvider<SimpleParticleType> flame(SpriteSet sprites) {
        return (type, level, x, y, z, xd, yd, zd) -> new RimeBreathParticle(level, x, y, z, xd, yd, zd, sprites, false);
    }

    static ParticleProvider<SimpleParticleType> smoke(SpriteSet sprites) {
        return (type, level, x, y, z, xd, yd, zd) -> new RimeBreathParticle(level, x, y, z, xd, yd, zd, sprites, true);
    }
}
