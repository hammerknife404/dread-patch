package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.client.RisenDreadClient;
import io.github.hammerknife404.dreadpatch.magic.PhantomRules;
import io.github.hammerknife404.dreadpatch.magic.RisenRules;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Creatures risen Dread (0.63.0, the owner's dump, asked what turns them: "Dying to whiteout dominion or other
 * conditions that would turn a corpse into a dread mob"; and "intended for overworld only or places that have
 * water and aquatic life"). Where the Dread raise the dead (her storm, so far):
 * <ul>
 * <li>a bat or parrot ({@link #AS_PHANTOMS}) rises as a {@link MiniatureDreadPhantom}, hers;</li>
 * <li>a water creature ({@code #minecraft:aquatic}, or any {@link WaterAnimal}) rises as a {@link RimeEel}, hers
 * (0.71.0; from 0.63.0 to 0.70.0 it rose as itself made Dread).</li>
 * </ul>
 * In the overworld's underground water a water creature still stirs now and then as itself made Dread
 * ({@link #makeDread}): marked {@link #RISEN} in its persistent data, twice its health, much faster
 * ({@link RisenRules}), hunting players and what she hunts, its bite the dread phantom's (Rimefrost and
 * Withering Rime, each by chance) and sized by its own health. Drawn frost-tinted ({@link RisenDreadClient},
 * told by {@link Marked}).
 * They're of the Dread to this mod ({@link #dread}): they refuse its rime and storms, and the Dread's magic
 * spares them. (Ice and Fire's own dread mobs still take them for prey: its tests know only its own.) Now and
 * then they also stir on their own, in caves and underground water, and Rime Eels in the Dreadlands' dark water
 * ({@code world/RisenSpawns}).
 */
public final class RisenDread {
    public static final String RISEN = "dreadpatch:risen";
    /** Who raised it, whose target it takes. */
    public static final String RAISED_BY = "dreadpatch:risen_by";
    public static final TagKey<EntityType<?>> AS_PHANTOMS = TagKey.create(Registries.ENTITY_TYPE, DreadPatch.id("rise_as_miniature_phantoms"));
    static final ResourceLocation HEALTH = DreadPatch.id("risen_health");
    static final ResourceLocation SPEED = DreadPatch.id("risen_speed");

    /** To a client: this entity is risen Dread (drawn so). */
    public record Marked(int entity) implements CustomPacketPayload {
        public static final Type<Marked> TYPE = new Type<>(DreadPatch.id("risen"));
        public static final StreamCodec<ByteBuf, Marked> CODEC = ByteBufCodecs.VAR_INT.map(Marked::new, Marked::entity);

        @Override
        public Type<Marked> type() {
            return TYPE;
        }
    }

    private RisenDread() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) -> event.registrar("1")
                .playToClient(Marked.TYPE, Marked.CODEC, (payload, context) -> RisenDreadClient.mark(payload.entity())));
        gameBus.addListener(RisenDread::onJoin);
        gameBus.addListener(RisenDread::onStartTracking);
    }

    /** Whether it's risen Dread (on the server: the mark isn't synced). */
    public static boolean is(Entity entity) {
        return entity instanceof Mob && entity.getPersistentData().getBoolean(RISEN);
    }

    /** Of the Dread, to this mod: Ice and Fire's (and ours), or risen. */
    public static boolean dread(Entity entity) {
        return entity instanceof IDreadMob || is(entity);
    }

    /** Whether the dead rise as one of these: a bat or parrot, or a water creature. */
    public static boolean risesAs(LivingEntity dead) {
        return dead.getType().is(AS_PHANTOMS) || dead instanceof Mob && aquatic(dead);
    }

    static boolean aquatic(Entity entity) {
        return entity.getType().is(EntityTypeTags.AQUATIC) || entity instanceof WaterAnimal;
    }

    /** What {@code dead} rises as (a miniature dread phantom or a Rime Eel), raised by {@code raiser}, not yet added; null if it rises as something else. */
    @Nullable
    public static Mob rise(ServerLevel level, LivingEntity dead, @Nullable LivingEntity raiser) {
        if (dead.getType().is(AS_PHANTOMS)) {
            MiniatureDreadPhantom phantom = ModRegistries.MINIATURE_DREAD_PHANTOM.get().create(level);
            if (phantom == null) {
                return null;
            }
            phantom.moveTo(dead.getX(), dead.getY() + 0.5, dead.getZ(), dead.getYRot(), 0.0F);
            EventHooks.finalizeMobSpawn(phantom, level, level.getCurrentDifficultyAt(dead.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            if (raiser != null) {
                phantom.setSummoner(raiser);
            }
            return phantom;
        }
        // A water creature rises a Rime Eel (0.71.0, the owner's spec: "any aquatic-type creature slain during the Dread
        // Queen's Whiteout Dominion becomes a Rime Eel"), its raiser's.
        if (!(dead instanceof Mob) || !aquatic(dead)) {
            return null;
        }
        RimeEel eel = ModRegistries.RIME_EEL.get().create(level);
        if (eel == null) {
            return null;
        }
        eel.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(eel, level, level.getCurrentDifficultyAt(dead.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        if (raiser != null) {
            eel.getPersistentData().putUUID(RAISED_BY, raiser.getUUID());
        }
        return eel;
    }

    /** Makes {@code mob} risen Dread: marked, its health doubled and whole, much faster. Before it's added. */
    public static void makeDread(Mob mob, @Nullable LivingEntity raiser) {
        CompoundTag data = mob.getPersistentData();
        data.putBoolean(RISEN, true);
        if (raiser != null) {
            data.putUUID(RAISED_BY, raiser.getUUID());
        }
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, RisenRules.HEALTH_TIMES - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(SPEED, RisenRules.SPEED_TIMES - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        mob.setHealth(mob.getMaxHealth());
    }

    /** Goals aren't saved: every time a risen one joins a level it's given its hunt again, and its fear taken. */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob) || !is(mob) || mob instanceof DreadPhantom) {
            return;
        }
        mob.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal || goal instanceof AvoidEntityGoal<?>);
        mob.goalSelector.addGoal(1, new BiteGoal(mob));
        mob.targetSelector.addGoal(1, new RaiserTargetGoal(mob));
        if (mob instanceof PathfinderMob pathfinder) {
            mob.targetSelector.addGoal(2, new HurtByTargetGoal(pathfinder));
        }
        mob.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(mob, Player.class, true));
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (is(event.getTarget()) && event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new Marked(event.getTarget().getId()));
        }
    }

    /** Its raiser, if they're alive in this level. */
    @Nullable
    static LivingEntity raiser(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.hasUUID(RAISED_BY) || !(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID id = data.getUUID(RAISED_BY);
        return level.getEntity(id) instanceof LivingEntity raiser && raiser.isAlive() ? raiser : null;
    }

    /** Takes its raiser's target, every second, while it has none of its own. */
    static final class RaiserTargetGoal extends Goal {
        private final Mob mob;

        RaiserTargetGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.mob.tickCount % 20 != 0 || this.mob.getTarget() != null && this.mob.getTarget().isAlive()) {
                return false;
            }
            LivingEntity raiser = raiser(this.mob);
            LivingEntity target = raiser instanceof Mob commander ? commander.getTarget() : null;
            if (target != null && target.isAlive() && !dread(target)) {
                this.mob.setTarget(target);
            }
            return false;
        }
    }

    /**
     * Swims (or walks) at its target and bites: its own damage sized by its health ({@link RisenRules#bite}),
     * as a mob's attack, then the dread phantom's two chances. Its own, since a fish has no attack damage to
     * attack with.
     */
    static final class BiteGoal extends Goal {
        private final Mob mob;
        private int cooldown;
        private int repath;

        BiteGoal(Mob mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && target.isAlive() && !dread(target);
        }

        @Override
        public void start() {
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.repath <= 0) {
                this.repath = 10;
                this.mob.getNavigation().moveTo(target, 1.0);
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            double reach = this.mob.getBbWidth() * 2.0 + target.getBbWidth();
            if (this.mob.distanceToSqr(target) <= reach * reach && this.mob.hasLineOfSight(target)) {
                this.cooldown = RisenRules.BITE_TICKS;
                this.mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                AttributeInstance health = this.mob.getAttribute(Attributes.MAX_HEALTH);
                float bite = RisenRules.bite(health == null ? this.mob.getMaxHealth() : health.getBaseValue());
                if (target.hurt(this.mob.damageSources().mobAttack(this.mob), bite)) {
                    if (PhantomRules.rimefrost(this.mob.getRandom().nextFloat())) {
                        target.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, PhantomRules.RIMEFROST_TICKS,
                                io.github.hammerknife404.dreadpatch.effect.RimefrostEffect.HEAVY), this.mob);
                    }
                    if (PhantomRules.witheringRime(this.mob.getRandom().nextFloat())) {
                        target.addEffect(new MobEffectInstance(ModRegistries.WITHERING_RIME, PhantomRules.WITHERING_TICKS), this.mob);
                    }
                }
            }
        }
    }
}
