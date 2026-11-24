package io.github.hammerknife404.dreadpatch.entity;

import net.minecraft.world.level.GameRules;
import io.github.hammerknife404.dreadpatch.world.DreadlandsStorms;
import com.iafenvoy.iceandfire.config.IafCommonConfig;
import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.ai.DragonAITargetNonTamedGoal;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.iafenvoy.iceandfire.entity.util.dragon.IafDragonAttacks;
import com.iafenvoy.iceandfire.item.DragonEggItem;
import com.iafenvoy.iceandfire.item.block.entity.DragonForgeInputBlockEntity;
import com.iafenvoy.uranus.animation.Animation;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.block.RimefrostForgeInputBlockEntity;
import io.github.hammerknife404.dreadpatch.magic.DragonBreath;
import io.github.hammerknife404.dreadpatch.magic.ForgeRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;
import java.util.UUID;

/**
 * A Rimefrost dragon (0.37.0), in the owner's taxonomy a subtype of frost dragon: an Ice and Fire ice
 * dragon of the Dread, undead, rotted to the bone. Ice and Fire's ice dragon underneath (its model,
 * animations, growth, flight, riding and AI come with {@link IceDragonEntity}; Ice and Fire's renderer
 * picks the ice dragon model by {@code instanceof}), in its own ice dragon colour, drawn rotted by
 * {@code RimefrostDragonRenderer}. Every Rimefrost dragon has the subtype's traits:
 * <ul>
 * <li>one of the Dread: allied with the queen and her court, refusing Withering Rime, never fighting them;</li>
 * <li>its breath is Withering Rime, a cone up to 32 blocks ({@link #performNormalBreathAttack});</li>
 * <li>its bite leaves Withering Rime II ({@code SyndrigossaEvents});</li>
 * <li>its charge is Shatter-Spine ({@link ShatterSpineCore}), which it looses on its own every 10 s in the air;</li>
 * <li>undead (tagged; no Wither, in {@code SyndrigossaEvents}; it never sleeps), hunting players
 * whatever Ice and Fire's config says, with a Dread corpse;</li>
 * <li>her animation layers and her breath's particles;</li>
 * <li>its breath lights the Rimefrost dragonforge, and a tame one is drawn to breathe on one
 * ({@link #updateBurnTarget}; 0.59.0); Ice and Fire's forges let it be.</li>
 * </ul>
 * Syndrigossa is the unique, named one ({@link Syndrigossa}): the queen's mount, the boss.
 */
public class RimefrostDragon extends IceDragonEntity implements IDreadMob {
    /** Its bite's Withering Rime: II (amplifier 1, withering twice as fast), for 4.5 s, as the queen's bolt lasts. */
    public static final int BITE_RIME_TICKS = 90;
    public static final int BITE_RIME_AMPLIFIER = 1;
    /** Its own Shatter-Spine in the air: ticks between them, the range it looses one at, and how long its charge (Ice and Fire's) runs. */
    static final int SPINE_COOLDOWN = 200;
    static final double SPINE_MIN_RANGE = 6.0;
    static final double SPINE_MAX_RANGE = 32.0;
    static final int SPINE_CHARGE_TICKS = 24;
    private int spineCooldown = 60;
    private int spineCharging;
    /** On the ground with a live target, the chance each tick it takes off: about every 3 s (Ice and Fire: 1 in 1,500, and only after a target 5+ blocks above or below). */
    static final int TAKEOFF_CHANCE = 60;
    /** Breath particles a tick: sent from its mouth to carry along the cone, and set in it, so a short burst shows it whole. */
    static final int BREATH_PARTICLES_SENT = 10;
    static final int BREATH_PARTICLES_SET = 10;

    /** Its corpse (Dread: no flesh, blood or heart to take), its bones, and what it leaves in place of a heart. */
    public static final ResourceLocation CORPSE_LOOT = DreadPatch.id("entities/rimefrost_dragon/corpse");
    public static final ResourceLocation BONES_LOOT = DreadPatch.id("entities/rimefrost_dragon/skeleton");
    public static final ResourceLocation HEART_LOOT = DreadPatch.id("entities/rimefrost_dragon/heart");

    /** Set while Ice and Fire takes a harvest from its corpse, so its heart and egg can be swapped. */
    private boolean harvesting;
    /** Saved: who raised it with Create Rimefrost Dragon ({@code CreateRimefrostDragonSpell}), if anyone. */
    public static final String RAISED_BY_KEY = "RaisedBy";
    @Nullable
    private UUID raisedBy;

    public RimefrostDragon(EntityType<? extends RimefrostDragon> type, Level level) {
        super(type, level);
    }

    /**
     * Whom it answers to among the Dread: whoever raised it, if that wasn't a player (a player's is
     * their pet, tamed to them, as Ice and Fire's dragons are) and they're loaded; else none.
     * Syndrigossa answers to her queen.
     */
    @Nullable
    @Override
    public Entity getCommander() {
        Entity raiser = this.raiser();
        return raiser instanceof Player ? null : raiser;
    }

    /** Who raised it with Create Rimefrost Dragon, while loaded (server side). */
    @Nullable
    public Entity raiser() {
        return this.raisedBy != null && this.level() instanceof ServerLevel level ? level.getEntity(this.raisedBy) : null;
    }

    @Nullable
    public UUID getRaisedBy() {
        return this.raisedBy;
    }

    public void setRaisedBy(@Nullable UUID raisedBy) {
        this.raisedBy = raisedBy;
    }

    /** Sent back: it crumbles away, nothing left, no corpse and no loot (Create Rimefrost Dragon's recast). */
    public void crumble() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY(0.5), this.getZ(), 60, this.getBbWidth() / 2, this.getBbHeight() / 2, this.getBbWidth() / 2, 0.05);
            level.sendParticles(ModRegistries.RIME_SMOKE.get(), this.getX(), this.getY(0.5), this.getZ(), 30, this.getBbWidth() / 2, this.getBbHeight() / 2, this.getBbWidth() / 2, 0.02);
            this.playSound(this.getDeathSound(), 2.0F, 0.6F);
        }
        this.ejectPassengers();
        this.discard();
    }

    /** Its corpse, bones and heart tables: Syndrigossa's are her own (her heart holds her egg). */
    protected ResourceLocation corpseLoot() {
        return CORPSE_LOOT;
    }

    protected ResourceLocation bonesLoot() {
        return BONES_LOOT;
    }

    protected ResourceLocation heartLoot() {
        return HEART_LOOT;
    }

    /**
     * Rimefrost dragons are undead: they never sleep (0.40.0, the owner). Ice and Fire's sleep is this
     * pose (its {@code SLEEPING} flag, saved as {@code Sleeping}; sitting is {@code setOrderedToSit}),
     * which its {@code IafDragonLogic} sets at night (config {@code dragon.sleep}) unless it's time to
     * wake: never set, it's never asleep, and it's always time to wake. One saved asleep wakes as it loads.
     */
    @Override
    public void setInSittingPose(boolean sleeping) {
        super.setInSittingPose(false);
    }

    @Override
    public boolean isSleeping() {
        return false;
    }

    @Override
    public boolean isTimeToWake() {
        return true;
    }

    /** Anyone on its back: prey in its jaws, or a rider. Syndrigossa doesn't count her queen. */
    protected boolean carriesOthers() {
        return !this.getPassengers().isEmpty();
    }

    /**
     * A dragon made from a tag ({@code /summon} with NBT skips {@code finalizeSpawn}) reads its
     * health while its max is still a hatchling's 20 (the tag has no saved attributes, which a saved
     * dragon's does), and Ice and Fire only grows the max to its age afterwards, here: summoned
     * full-grown Syndrigossa had 20 of 419 and fell to one warden hit, and {@code Health:300} came out
     * 20. Ice and Fire's own ice dragons do the same. So once its max is set: the tag's health, or whole.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setHealth(tag.contains("Health", Tag.TAG_ANY_NUMERIC) ? tag.getFloat("Health") : this.getMaxHealth());
        this.raisedBy = tag.hasUUID(RAISED_BY_KEY) ? tag.getUUID(RAISED_BY_KEY) : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.raisedBy != null) {
            tag.putUUID(RAISED_BY_KEY, this.raisedBy);
        }
    }

    /**
     * It hunts players whatever Ice and Fire's {@code neutralToPlayer} says (the owner's pack sets
     * it, so its wild dragons leave players be until struck): it's of the Dread. Ice and Fire's own
     * wild-dragon targeting, for players only and without that setting; never a creative or spectator
     * player, and a tame one ({@link DragonAITargetNonTamedGoal} stands down) follows its owner.
     */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(5, new DragonAITargetNonTamedGoal<>(this, Player.class, false,
                entity -> entity instanceof Player player && !player.isCreative() && !player.isSpectator()));
    }

    /** As Ice and Fire's ice dragon picks: bones from halfway through the harvest (when it's drawn as a skeleton). */
    @Override
    public ResourceLocation getDeadLootTable() {
        return this.getDeathStage() >= this.getAgeInDays() / 5 / 2 ? this.bonesLoot() : this.corpseLoot();
    }

    /**
     * Its corpse is harvested as Ice and Fire's dragons are (empty hand, a take each click), but it's
     * undead: a bottle draws no blood, its heart is dread ({@link #heartLoot}), and it carries no ice
     * dragon's egg (Syndrigossa's own egg is in her heart's table).
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isModelDead()) {
            return super.mobInteract(player, hand);
        }
        ItemStack held = player.getMainHandItem().isEmpty() ? player.getItemInHand(hand) : player.getMainHandItem();
        if (held.is(Items.GLASS_BOTTLE)) {
            return InteractionResult.PASS;
        }
        this.harvesting = true;
        try {
            return super.mobInteract(player, hand);
        } finally {
            this.harvesting = false;
        }
    }

    @Nullable
    @Override
    public ItemEntity spawnAtLocation(ItemStack stack, float offsetY) {
        if (this.harvesting && this.level() instanceof ServerLevel level) {
            if (stack.getItem() instanceof DragonEggItem) {
                return null;
            }
            if (stack.is(this.getHeartItem().asItem())) {
                ItemEntity last = null;
                for (ItemStack drop : this.rollLoot(level, this.heartLoot())) {
                    last = super.spawnAtLocation(drop, offsetY);
                }
                return last;
            }
        }
        return super.spawnAtLocation(stack, offsetY);
    }

    /** One of its loot tables, rolled as Ice and Fire rolls a dragon's corpse table. */
    private List<ItemStack> rollLoot(ServerLevel level, ResourceLocation table) {
        LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        return loot.getRandomItems(params);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !this.isAlive() || this.isModelDead()) {
            return;
        }
        // A target handed to it (by its queen, or kept from before) isn't rechecked by a target goal;
        // Ice and Fire breathes at whatever it is: a player gone creative or spectator included.
        LivingEntity current = this.getTarget();
        if (current != null && (!current.isAlive() || !this.canAttack(current))) {
            this.setTarget(null);
        }
        // Raised by the queen (or another of the Dread): her target is its, as her court's is.
        if (this.tickCount % 20 == 0 && this.getCommander() instanceof Mob commander && commander.getTarget() != null
                && commander.getTarget().isAlive() && this.getTarget() != commander.getTarget() && this.canAttack(commander.getTarget())) {
            this.setTarget(commander.getTarget());
        }
        this.beforeAttacks();
        LivingEntity hunted = this.getTarget();
        if (hunted != null && hunted.isAlive() && !this.isFlying() && !this.isHovering() && !this.carriesOthers() && !this.isOrderedToSit()
                && !this.isSleeping() && this.isAllowedToTriggerFlight() && this.random.nextInt(TAKEOFF_CHANCE) == 0) {
            this.takeOff();
        }
        this.shatterSpine(hunted);
    }

    /** Each server tick it's alive, before it takes off or looses Shatter-Spine: Syndrigossa's own business. */
    protected void beforeAttacks() {
    }

    /**
     * Its own Shatter-Spine, every {@link #SPINE_COOLDOWN} ticks in the air with its target in sight 6-32
     * blocks off: Ice and Fire's charge attack ({@code performChargeAttack}, called each tick of the
     * charge: its animation, then {@code createCharge} at its 20th tick), whatever air attack Ice and
     * Fire has picked. Its Hover Blast came round only now and then (1-2 in 40 s, the first 14-25 s in),
     * since Ice and Fire swaps air attacks in many places; the owner never saw one.
     */
    private void shatterSpine(@Nullable LivingEntity target) {
        boolean airborne = this.isFlying() || this.isHovering();
        if (this.spineCharging > 0) {
            this.spineCharging--;
            if (target != null && target.isAlive() && airborne) {
                this.performChargeAttack(target.getX(), target.getY(), target.getZ());
            } else {
                this.spineCharging = 0;
            }
            return;
        }
        if (this.spineCooldown > 0) {
            this.spineCooldown--;
            return;
        }
        if (target != null && target.isAlive() && airborne && !this.carriesOthers()) {
            double distance = this.distanceTo(target);
            if (distance >= SPINE_MIN_RANGE && distance <= SPINE_MAX_RANGE && this.hasLineOfSight(target)) {
                this.spineCharging = SPINE_CHARGE_TICKS;
                this.spineCooldown = SPINE_COOLDOWN;
            }
        }
    }

    /**
     * Up, as Ice and Fire takes a dragon off (hovering first, then flying), opening with Shatter-Spine:
     * Ice and Fire picks again after the charge. Otherwise it rarely showed: it's its only charge and
     * only in the air, and dragons can't target players in creative or spectator.
     */
    void takeOff() {
        this.airAttack = IafDragonAttacks.Air.HOVER_BLAST;
        this.setHovering(true);
        this.setInSittingPose(false);
        this.flyHovering = 0;
        this.hoverTicks = 0;
        this.flyTicks = 0;
    }

    /**
     * Its air attacks weighted toward the strafing fly-by (Ice and Fire's Scorch Stream, 50%) and
     * Shatter-Spine (its Hover Blast, the only air attack that charges, 35%), over the dive (Tackle,
     * 15%); Ice and Fire picks each a third of the time. Ground attacks stay its own.
     */
    @Override
    public void randomizeAttacks() {
        super.randomizeAttacks();
        int roll = this.getRandom().nextInt(20);
        this.airAttack = roll < 10 ? IafDragonAttacks.Air.SCORCH_STREAM : roll < 17 ? IafDragonAttacks.Air.HOVER_BLAST : IafDragonAttacks.Air.TACKLE;
    }

    /**
     * Its breath, a cone from its head where Ice and Fire's is a thin stream: each tick it breathes,
     * particles spread through it, and everyone it may harm and can see in it, up to where it lands,
     * or within reach of where it lands, takes a breath hit ({@link #breathHit}). It grows out over
     * Ice and Fire's breathing, reaching its full length in half a second (its stream takes two).
     */
    @Override
    protected void performNormalBreathAttack(double x, double y, double z) {
        this.getNavigation().stop();
        this.burnParticleX = x;
        this.burnParticleY = y;
        this.burnParticleZ = z;
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 head = this.getHeadPosition();
        Vec3 aim = new Vec3(x, y, z).subtract(head);
        if (aim.lengthSqr() < 0.25) {
            return;
        }
        Vec3 axis = aim.normalize();
        double length = DragonBreath.coneLength(aim.length(), this.burnProgress);
        double splash = DragonBreath.breathRadius(this.getDragonStage());
        // Every particle ends where the breath reaches (DragonBreathTest runs vanilla's particle motion on these).
        for (int i = 0; i < BREATH_PARTICLES_SENT; i++) {
            Vec3 v = this.spread(axis);
            double carry = DragonBreath.sentDistance(length, splash, this.random.nextDouble());
            ParticleOptions particle = this.createBreathParticle();
            level.sendParticles(particle, head.x, head.y, head.z, 0, v.x, v.y, v.z, breathParticleSpeed(particle, carry));
        }
        for (int i = 0; i < BREATH_PARTICLES_SET; i++) {
            double along = length * (0.15 + 0.85 * this.random.nextDouble());
            Vec3 at = head.add(this.spread(axis).scale(along));
            double drift = DragonBreath.driftDistance(length, along, splash, this.random.nextDouble());
            ParticleOptions particle = this.createBreathParticle();
            level.sendParticles(particle, at.x, at.y, at.z, 0, axis.x, axis.y, axis.z, breathParticleSpeed(particle, drift));
        }
        Vec3 end = head.add(axis.scale(length));
        BlockHitResult block = level.clip(new ClipContext(head, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 lands = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        double reach = head.distanceTo(lands);
        int stage = this.getDragonStage();
        double radius = DragonBreath.breathRadius(stage);
        float damage = DragonBreath.breathDamage(stage, attackDamageIce());
        DamageSource source = ModRegistries.damageSource(level, ModRegistries.WITHERING_BREATH_DAMAGE, this, this);
        AABB box = new AABB(head, lands).inflate(reach * Math.tan(DragonBreath.CONE_HALF_ANGLE) + radius);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !this.hasPassenger(e) && RimefireCrown.canHarm(this, e))) {
            Vec3 at = victim.getBoundingBox().getCenter().subtract(head);
            boolean inCone = DragonBreath.inCone(at.x, at.y, at.z, axis.x, axis.y, axis.z, reach, victim.getBbWidth() / 2.0);
            boolean splashed = victim.getBoundingBox().distanceToSqr(lands) <= radius * radius;
            // Nothing of it reaches past its max distance from its head, the splash included.
            boolean inReach = victim.getBoundingBox().distanceToSqr(head) <= DragonBreath.CONE_MAX_LENGTH * DragonBreath.CONE_MAX_LENGTH;
            if ((inCone || splashed) && inReach && this.hasLineOfSight(victim)) {
                this.breathHit(victim, source, damage);
            }
        }
        BlockPos center = BlockPos.containing(lands);
        boolean forge = this.fuelForges(level, center);
        if (this.tickCount % 4 == 0) {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                RimefireCrown.drinkFire(level, pos);
            }
            // No rimefire on a forge: it would burn whoever's working it.
            if (block.getType() != HitResult.Type.MISS && !forge) {
                this.maybeIgnite(level, center);
            }
        }
    }

    /**
     * A Rimefrost dragonforge's aperture where its breath lands lights its forge (0.59.0), as Ice and
     * Fire's breath lights its own: every aperture within {@link ForgeRules#breathReach} of the spot, each
     * tick it breathes. Any Rimefrost dragon's breath does, tame or not. Whether it reached one.
     */
    private boolean fuelForges(ServerLevel level, BlockPos center) {
        int reach = ForgeRules.breathReach(this.getDragonStage());
        boolean found = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            if (level.getBlockEntity(pos) instanceof RimefrostForgeInputBlockEntity aperture) {
                aperture.breathedOn(this);
                found = true;
            }
        }
        return found;
    }

    /**
     * Breathing on a Rimefrost dragonforge it's been drawn to ({@code burningTarget}, set by the forge's
     * aperture): Ice and Fire's own for its forges, to the letter (not asleep, dead or a hatchling; the
     * forge whole with something to make; within 115 a stage, squared; in sight). Ice and Fire's forges
     * take it for an ice dragon and draw it too, but its breath never reaches their code, and the dread
     * recipes are only the new forge's (the owner's call): it lets theirs go.
     */
    @Override
    public void updateBurnTarget() {
        BlockPos at = this.burningTarget;
        if (at != null && this.level().getBlockEntity(at) instanceof RimefrostForgeInputBlockEntity aperture) {
            Vec3 c = Vec3.atCenterOf(at);
            if (!this.isSleeping() && !this.isModelDead() && !this.isBaby() && aperture.ready()
                    && ForgeRules.withinBreath(this.distanceToSqr(c), this.getDragonStage()) && this.canPositionBeSeen(c.x, c.y, c.z)) {
                this.getLookControl().setLookAt(c.x, c.y, c.z, 180.0F, 180.0F);
                this.breathFireAtPos(at);
                this.setBreathingFire(true);
                return;
            }
            this.burningTarget = null;
            this.setBreathingFire(false);
            return;
        }
        if (at != null && this.level().getBlockEntity(at) instanceof DragonForgeInputBlockEntity) {
            this.burningTarget = null;
            return;
        }
        super.updateBurnTarget();
    }

    /** Only while it's at a Rimefrost dragonforge (Ice and Fire's wander and idle-look goals stand down meanwhile). */
    @Override
    public boolean isFuelingForge() {
        return this.burningTarget != null && this.level().getBlockEntity(this.burningTarget) instanceof RimefrostForgeInputBlockEntity;
    }

    /** The speed to send one of its breath particles at to carry {@code distance}: vanilla's snowflake goes further for its speed. */
    private static double breathParticleSpeed(ParticleOptions particle, double distance) {
        return particle == ParticleTypes.SNOWFLAKE ? DragonBreath.snowSpeed(distance) : DragonBreath.particleSpeed(distance);
    }

    /** A direction within the breath cone round {@code axis}, spread evenly over it. */
    private Vec3 spread(Vec3 axis) {
        Vec3 side = Math.abs(axis.y) < 0.9 ? axis.cross(new Vec3(0, 1, 0)).normalize() : axis.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = axis.cross(side);
        double cosTheta = 1.0 - this.random.nextDouble() * (1.0 - Math.cos(DragonBreath.CONE_HALF_ANGLE));
        double sinTheta = Math.sqrt(1.0 - cosTheta * cosTheta);
        double phi = this.random.nextDouble() * Math.PI * 2.0;
        return axis.scale(cosTheta).add(side.scale(Math.cos(phi) * sinTheta)).add(up.scale(Math.sin(phi) * sinTheta));
    }

    /** One breath hit: its breath damage (and its siphon, in {@code SyndrigossaEvents}), then Withering Rime. */
    private void breathHit(LivingEntity victim, DamageSource source, float damage) {
        if (victim.hurt(source, damage) && victim.isAlive()) {
            victim.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, DragonBreath.RIME_TICKS), this);
        }
    }

    /** Ice and Fire's ice breath multiplier ({@code dragon.attackDamageIce}; 2.1 in the owner's pack). */
    public static double attackDamageIce() {
        return IafCommonConfig.INSTANCE.dragon.attackDamageIce.getValue();
    }

    /**
     * Its breath's splash where Ice and Fire would have frozen a spot (its {@code DragonFireDamageWorldEvent},
     * now only from what still reaches Ice and Fire's breath code: its own breath is {@link #performNormalBreathAttack}):
     * everyone it may harm and can see within reach takes an ice dragon's breath hit of its stage
     * and Withering Rime (each hit also siphons {@link DragonBreath#SIPHON} into it, in
     * {@code SyndrigossaEvents}), and fire there is drunk. Nothing freezes.
     */
    void witheringBreath(Vec3 at) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        int stage = this.getDragonStage();
        double radius = DragonBreath.breathRadius(stage);
        float damage = DragonBreath.breathDamage(stage, attackDamageIce());
        DamageSource source = ModRegistries.damageSource(level, ModRegistries.WITHERING_BREATH_DAMAGE, this, this);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != this && e.isAlive() && !this.hasPassenger(e) && RimefireCrown.canHarm(this, e) && this.hasLineOfSight(e))) {
            this.breathHit(victim, source, damage);
        }
        BlockPos center = BlockPos.containing(at);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            RimefireCrown.drinkFire(level, pos);
        }
        this.maybeIgnite(level, center);
    }

    /** Now and then, a rimefire patch where its breath lands, as lightning lights in the Dreadlands (0.55.0). */
    private void maybeIgnite(ServerLevel level, BlockPos at) {
        if (this.random.nextInt(DragonBreath.IGNITE_ONE_IN) == 0 && level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            DreadlandsStorms.ignite(level, at);
        }
    }

    /** Cold dread fire, soot and a little snow, in place of the ice dragon's frost: our own particles, several times vanilla's size. */
    @Override
    public ParticleOptions createBreathParticle() {
        int pick = this.random.nextInt(10);
        return pick < 6 ? ModRegistries.RIME_FLAME.get() : pick < 9 ? ModRegistries.RIME_SMOKE.get() : ParticleTypes.SNOWFLAKE;
    }

    /**
     * Its animations in three layers where Ice and Fire's dragons have one. Its attack animations
     * move only the head, neck, jaw, claws and wings (held spread), so with one layer, attacking in
     * the air froze the body, tail and legs in the model's rest pose (the owner saw Syndrigossa "posed
     * still in a sitting idle animation except the mouth and head"), and Ice and Fire played the
     * hover-in-place breath even on a fly-by. So: the fly-by breath or charge under its body (the
     * body's flight wins where both move a bone, so its wings beat and only the jaw and claws take the
     * breath's pose), its body (Ice and Fire's choice of flight, swimming, ground, walking, sitting,
     * sleeping, dead), and over it the hovering or standing breath and charge and its other actions
     * (bite, shake, wing blast, roars, tail), as Ice and Fire plays them.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "flyby_attack", 2, this::flybyAttack));
        controllers.add(new AnimationController<>(this, "body", 2, this::body));
        controllers.add(new AnimationController<>(this, "attack", 2, this::attack));
    }

    private boolean airborne() {
        return this.isFlying() || this.isHovering() || this.flyProgress > 0.0F || this.hoverProgress > 0.0F;
    }

    /** Flying past, not hovering in place. */
    private boolean onFlyby() {
        return this.isFlying() && !this.isHovering();
    }

    private PlayState flybyAttack(AnimationState<RimefrostDragon> state) {
        if (!this.isModelDead() && this.onFlyby()) {
            if (this.getAnimation() == ANIMATION_FIRECHARGE) {
                return once(state, "attack_stream_charge");
            }
            if (this.isBreathingFire()) {
                return loop(state, "attack_stream_breath", 1.0F);
            }
        }
        return PlayState.STOP;
    }

    private PlayState body(AnimationState<RimefrostDragon> state) {
        if (this.isModelDead()) {
            return loop(state, "dead", 1.0F);
        }
        if ((this.isSwimming || this.isInWater()) && this.swimProgress > 0.0F) {
            return loop(state, "swim", 1.0F);
        }
        if (this.airborne()) {
            return loop(state, "flight", 0.4F);
        }
        if (this.isSleeping() || this.sleepProgress > 0.0F) {
            return loop(state, "sleeping", 1.0F);
        }
        if (this.isOrderedToSit()) {
            return loop(state, "sitting", 1.0F);
        }
        return state.isMoving() ? loop(state, "walk", 0.4F) : loop(state, "ground", 1.0F);
    }

    private PlayState attack(AnimationState<RimefrostDragon> state) {
        if (this.isModelDead() || this.isSleeping() || this.onFlyby()) {
            return PlayState.STOP;
        }
        Animation action = this.getAnimation();
        if (action == ANIMATION_FIRECHARGE) {
            return once(state, "attack_blast_charge");
        }
        if (this.isBreathingFire()) {
            return loop(state, "attack_blast_breath", 1.0F);
        }
        String name = this.airborne() ? null : groundAction(action);
        return name != null ? once(state, name) : PlayState.STOP;
    }

    /** Whether it's biting: Ice and Fire's bite, or shaking prey in its jaws. */
    public boolean biting() {
        Animation action = this.getAnimation();
        return action == ANIMATION_BITE || action == ANIMATION_SHAKEPREY;
    }

    /** Ice and Fire's names for its dragons' one-off ground animations. */
    @Nullable
    private static String groundAction(Animation action) {
        if (action == ANIMATION_BITE || action == ANIMATION_EAT || action == ANIMATION_SPEAK) {
            return "bite";
        }
        if (action == ANIMATION_SHAKEPREY) {
            return "grab_shake";
        }
        if (action == ANIMATION_WINGBLAST) {
            return "wing_blast";
        }
        if (action == ANIMATION_ROAR) {
            return "roar";
        }
        if (action == ANIMATION_EPIC_ROAR) {
            return "epic_roar";
        }
        return action == ANIMATION_TAILWHACK ? "tail" : null;
    }

    private static PlayState loop(AnimationState<RimefrostDragon> state, String name, float speed) {
        state.setControllerSpeed(speed);
        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.icedragon." + name));
    }

    private static PlayState once(AnimationState<RimefrostDragon> state, String name) {
        state.setControllerSpeed(1.0F);
        return state.setAndContinue(RawAnimation.begin().thenPlay("animation.icedragon." + name));
    }

    /** Its charge (Ice and Fire's charge attack, its own or a rider's) is Shatter-Spine, bursting over its target. */
    @Override
    public Entity createCharge(double x, double y, double z) {
        return ShatterSpineCore.launch(this.level(), this, this.getHeadPosition(), new Vec3(x, y, z), ShatterSpineCore.aimOver(this.getTarget()));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof IDreadMob || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof IDreadMob) && super.canAttack(target);
    }

    @Override
    protected boolean shouldTarget(Entity entity) {
        return !(entity instanceof IDreadMob) && super.shouldTarget(entity);
    }
}
