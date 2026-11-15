package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.iafenvoy.uranus.animation.AnimationHandler;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ice and Fire's Dread Queen with what its class leaves out. Ice and Fire's other dread mobs call
 * {@link AnimationHandler#updateAnimations} every tick; hers never does, so the spawn animation it
 * sets in {@code finalizeSpawn} stayed at tick 0 and never reached a client.
 */
public class DreadQueen extends DreadQueenEntity {
    /** Ticks of {@link #ANIMATION_SPAWN} (40 long) she spends coming out of the ground. */
    public static final int RISE_TICKS = 30;

    public DreadQueen(EntityType<? extends DreadQueen> type, Level level) {
        super(type, level);
    }

    public boolean isRising() {
        return this.getAnimation() == ANIMATION_SPAWN && this.getAnimationTick() < RISE_TICKS;
    }

    /** No walking, turning or attacking while she's still coming out of the ground. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isRising();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.isRising()) {
            // The ground breaking around her, as for Ice and Fire's rising dread thralls.
            BlockState below = this.level().getBlockState(this.blockPosition().below());
            if (!below.isAir()) {
                for (int i = 0; i < 5; i++) {
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, below),
                            this.getX() + (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth(),
                            this.getBoundingBox().minY,
                            this.getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth(),
                            this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D);
                }
            }
        }
        // Counts the animation along on both sides; on its first server tick it also tells clients
        // it started, and at its end it goes back to NO_ANIMATION.
        AnimationHandler.INSTANCE.updateAnimations(this);
    }
}
