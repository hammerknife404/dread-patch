package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.DreadQueenEntity;
import com.iafenvoy.uranus.animation.AnimationHandler;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ice and Fire's Dread Queen with what its class leaves out. Ice and Fire's other dread mobs call
 * {@link AnimationHandler#updateAnimations} every tick; hers never does, so the spawn animation it
 * sets in {@code finalizeSpawn} stayed at tick 0 and never reached a client.
 */
public class DreadQueen extends DreadQueenEntity {
    /** Ticks of {@link #ANIMATION_SPAWN} (40 long) she spends coming out of the ground. */
    public static final int RISE_TICKS = 30;
    /** Her Withering Rime-Bolt: flat damage, then this share of the target's max health (a player's takes 8%). */
    public static final float BOLT_DAMAGE = 8.0F;
    public static final float BOLT_MAX_HEALTH_SHARE = 0.15F;
    public static final int BOLT_RIME_TICKS = 90;
    /** Ticks between her bolts, and the range (blocks) she shoots them from: not point-blank, where she uses her sword. */
    static final int BOLT_COOLDOWN = 80;
    static final double BOLT_MIN_RANGE = 3.0;
    static final double BOLT_MAX_RANGE = 32.0;

    /** Ticks between her Rimefire Crowns, how long each stands, and how far she casts one. */
    static final int CROWN_COOLDOWN = 300;
    static final int CROWN_LIFETIME = 100;
    static final double CROWN_RANGE = 20.0;
    /** After a crown, her next bolt comes this soon, to shatter it. */
    static final int CROWN_FOLLOW_UP = 25;

    private int boltCooldown = 40;
    private int crownCooldown = 100;
    private final DreadCourt court = new DreadCourt(this);

    public DreadQueen(EntityType<? extends DreadQueen> type, Level level) {
        super(type, level);
    }

    /** Ice and Fire's goals, but she fights from range ({@link KeepDistanceGoal}) instead of walking into melee. */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.removeAllGoals(goal -> goal instanceof MeleeAttackGoal);
        this.goalSelector.addGoal(2, new KeepDistanceGoal(this));
    }

    public boolean isRising() {
        return this.getAnimation() == ANIMATION_SPAWN && this.getAnimationTick() < RISE_TICKS;
    }

    /** No walking, turning or attacking while she's still coming out of the ground. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isRising();
    }

    /**
     * Her loot table always drops her sword and staff, so the ones in her hands never drop as
     * equipment too (vanilla's 8.5% chance, more with looting, would sometimes give two). Applies
     * whatever drop chances a saved queen carries.
     */
    @Override
    protected float getEquipmentDropChance(EquipmentSlot slot) {
        return slot.getType() == EquipmentSlot.Type.HAND ? 0.0F : super.getEquipmentDropChance(slot);
    }

    /** Her spells and her court, on top of her movement ({@link KeepDistanceGoal}). Not called while she's rising ({@link #isImmobile}). */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.boltCooldown > 0) {
            this.boltCooldown--;
        }
        if (this.crownCooldown > 0) {
            this.crownCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) {
            target = null;
        }
        this.court.tick(target);
        if (target == null) {
            return;
        }
        double distance = this.distanceTo(target);
        if (this.crownCooldown == 0 && distance <= CROWN_RANGE && this.hasLineOfSight(target)) {
            this.raiseCrown(target);
            this.crownCooldown = CROWN_COOLDOWN;
            this.boltCooldown = Math.min(this.boltCooldown, CROWN_FOLLOW_UP);
            return;
        }
        if (this.boltCooldown == 0 && distance >= BOLT_MIN_RANGE && distance <= BOLT_MAX_RANGE && this.hasLineOfSight(target)) {
            this.shootBolt(target);
            this.boltCooldown = BOLT_COOLDOWN;
        }
    }

    /** Rimefire Crown round her target, then a bolt to shatter it ({@link #CROWN_FOLLOW_UP}). */
    private void raiseCrown(LivingEntity target) {
        this.level().addFreshEntity(RimefireCrown.create(this.level(), this, target.position(), CROWN_LIFETIME));
        this.swing(InteractionHand.OFF_HAND);
        this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 0.5F);
    }

    private void shootBolt(LivingEntity target) {
        WitheringRimeBolt bolt = new WitheringRimeBolt(this.level(), this, BOLT_DAMAGE, BOLT_MAX_HEALTH_SHARE, BOLT_RIME_TICKS);
        bolt.setPos(this.getX(), this.getEyeY() - 0.4, this.getZ());
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0).subtract(bolt.position());
        bolt.shoot(aim.normalize());
        this.level().addFreshEntity(bolt);
        this.swing(InteractionHand.OFF_HAND);
        this.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.6F);
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
