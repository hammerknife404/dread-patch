package io.github.hammerknife404.dreadpatch.spell;

import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.registry.IafSounds;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import io.github.hammerknife404.dreadpatch.entity.RimefrostDragon;
import io.github.hammerknife404.dreadpatch.magic.RimefrostRaising;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.network.casting.SyncCooldownPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Create Rimefrost Dragon (0.38.0, the owner's design): an Ice spell that raises an Ice and Fire ice
 * dragon's corpse, the one the caster looks at within {@link #RANGE} blocks, as a Rimefrost dragon of
 * the same age (so the same stage), gender and colour ({@link #raise}). It rises with half (rounded
 * down) of its caster's missing health ({@link RimefrostRaising#risingHealth}), and is the caster's:
 * a player's tamed to them, anyone else's answering to them. One at a time: a new raising sends the
 * last back. Only the queen drops its scroll; she casts it herself, for nothing ({@code DreadQueen}).
 * <p>
 * A player pays {@link #MANA} and then all the mana they have left. The cast opens Iron's recast
 * window for {@link #WINDOW_TICKS} (its cooldown): within it, one recast, free as Iron's recasts are,
 * sends the dragon back and raises the corpse they're looking at now (the owner's: "recasting within
 * its cooldown unsummons the current one and allows the new target"). The cooldown then runs only for
 * what's left of the window ({@link #onRecastFinished}), so it's always 3 minutes from the first
 * cast. Iron's won't cast a spell with recasts from a scroll: it's cast from a spellbook.
 */
public class CreateRimefrostDragonSpell extends AbstractSpell {
    public static final int MANA = 500;
    /** Its cooldown, and the window its one recast is open for: 3 minutes. */
    public static final int WINDOW_TICKS = 3600;
    /** How far off the corpse may be. */
    public static final int RANGE = 24;

    private final ResourceLocation id = DreadPatch.id("create_rimefrost_dragon");
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(DreadSpells.RIMEFROST)
            .setMaxLevel(1)
            .setCooldownSeconds(WINDOW_TICKS / 20.0)
            .setAllowCrafting(false)
            .build();

    public CreateRimefrostDragonSpell() {
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.baseManaCost = MANA;
        this.manaCostPerLevel = 0;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return this.id;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return this.defaultConfig;
    }

    /** Rimefrost, whatever an older world's Iron's config says ({@link DreadSpells}). */
    @Override
    public SchoolType getSchoolType() {
        return DreadSpells.school();
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public boolean allowLooting() {
        return false;
    }

    /** The first cast and one recast. */
    @Override
    public int getRecastCount(int spellLevel, @Nullable LivingEntity entity) {
        return 2;
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new WindowData();
    }

    @Override
    public Optional<net.minecraft.sounds.SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        List<MutableComponent> info = new ArrayList<>();
        info.add(Component.translatable("ui.dreadpatch.distance", RANGE));
        info.add(Component.translatable("ui.dreadpatch.rises_with", Math.round(RimefrostRaising.HEALTH_SHARE * 100)));
        info.add(Component.translatable("ui.dreadpatch.all_mana"));
        return info;
    }

    /** An Ice and Fire ice dragon's corpse (not a Rimefrost dragon's) to raise. */
    public static boolean isCorpse(@Nullable Entity entity) {
        return entity instanceof IceDragonEntity dragon && !(entity instanceof RimefrostDragon) && dragon.isModelDead() && !dragon.isRemoved();
    }

    /** A player needs a corpse in their sights, or the cast doesn't happen (and costs nothing): Iron's targeting helper, with its "no target" hint. */
    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return Utils.preCastTargetHelper(level, entity, playerMagicData, this, RANGE, 0.35F, true, CreateRimefrostDragonSpell::isCorpse);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level instanceof ServerLevel server) {
            IceDragonEntity corpse = playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData data
                    && data.getTarget(server) instanceof IceDragonEntity targeted && isCorpse(targeted) ? targeted : findCorpse(server, entity);
            if (corpse != null) {
                raise(server, entity, corpse);
            }
            if (entity instanceof ServerPlayer player && !playerMagicData.getPlayerRecasts().hasRecastForSpell(this)) {
                // The first cast: Iron's has taken the 500; the rest of their mana goes too, and the window opens.
                playerMagicData.setMana(0.0F);
                PacketDistributor.sendToPlayer(player, new SyncManaPacket(playerMagicData));
                playerMagicData.getPlayerRecasts().addRecast(new RecastInstance(this.getSpellId(), spellLevel,
                        this.getRecastCount(spellLevel, entity), WINDOW_TICKS, castSource, new WindowData()), playerMagicData);
            }
        }
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    /** The cooldown runs only for what's left of the window: 3 minutes from the first cast, recast or not. */
    @Override
    public void onRecastFinished(ServerPlayer player, RecastInstance recast, RecastResult result, ICastDataSerializable castData) {
        int left = recast.getTicksRemaining();
        if (left > 0) {
            MagicData.getPlayerMagicData(player).getPlayerCooldowns().addCooldown(this, left);
            PacketDistributor.sendToPlayer(player, new SyncCooldownPacket(this.getSpellId(), left));
        }
    }

    /** A caster without Iron's pre-cast (a mob, {@code /cast}): the corpse in its sights. */
    @Nullable
    static IceDragonEntity findCorpse(ServerLevel level, LivingEntity caster) {
        Vec3 from = caster.getEyePosition();
        Vec3 to = from.add(caster.getLookAngle().scale(RANGE));
        HitResult hit = RaycastBuilder.begin(level, caster).start(from).end(to).checkForBlocks(true).bbInflation(0.35F)
                .filter(e -> isCorpse(e) || e instanceof PartEntity<?> part && isCorpse(part.getParent())).build();
        if (hit instanceof EntityHitResult entityHit) {
            Entity found = entityHit.getEntity() instanceof PartEntity<?> part ? part.getParent() : entityHit.getEntity();
            return isCorpse(found) ? (IceDragonEntity) found : null;
        }
        return null;
    }

    /**
     * Raises {@code corpse} as {@code caster}'s Rimefrost dragon: the same age, gender and colour, where
     * it lay; its health half the caster's missing health (at least 1); a player's tamed to them,
     * anyone else's answering to them. The caster's last one is sent back first.
     */
    public static RimefrostDragon raise(ServerLevel level, LivingEntity caster, IceDragonEntity corpse) {
        dismiss(level, caster);
        RimefrostDragon dragon = ModRegistries.RIMEFROST_DRAGON.get().create(level);
        if (dragon == null) {
            return null;
        }
        dragon.moveTo(corpse.getX(), corpse.getY(), corpse.getZ(), corpse.getYRot(), 0.0F);
        dragon.setYBodyRot(corpse.yBodyRot);
        dragon.setYHeadRot(corpse.getYHeadRot());
        dragon.setVariant(corpse.getVariant());
        dragon.setGender(corpse.isMale());
        dragon.setAgeInTicks(corpse.getAgeInTicks());
        dragon.refreshDirtyAttributes();
        dragon.refreshDimensions();
        dragon.setHealth(RimefrostRaising.risingHealth(caster.getMaxHealth(), caster.getHealth(), dragon.getMaxHealth()));
        dragon.setRaisedBy(caster.getUUID());
        dragon.setPersistenceRequired();
        if (caster instanceof Player player) {
            dragon.tame(player);
        }
        corpse.discard();
        level.addFreshEntity(dragon);
        level.sendParticles(ParticleTypes.SOUL, dragon.getX(), dragon.getY(0.5), dragon.getZ(), 80, dragon.getBbWidth() / 2, dragon.getBbHeight() / 2, dragon.getBbWidth() / 2, 0.06);
        level.sendParticles(ModRegistries.RIME_FLAME.get(), dragon.getX(), dragon.getY(0.5), dragon.getZ(), 60, dragon.getBbWidth() / 2, dragon.getBbHeight() / 2, dragon.getBbWidth() / 2, 0.04);
        level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(), IafSounds.DREAD_LICH_SUMMON.get(), SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, dragon.getX(), dragon.getY(), dragon.getZ(), dragon.getRoarSound(), SoundSource.HOSTILE, 3.0F, 0.6F);
        return dragon;
    }

    /** Sends back every Rimefrost dragon {@code caster} raised in this level (one at a time). */
    public static void dismiss(ServerLevel level, Entity caster) {
        List<RimefrostDragon> raised = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof RimefrostDragon dragon && caster.getUUID().equals(dragon.getRaisedBy()) && dragon.isAlive()) {
                raised.add(dragon);
            }
        }
        raised.forEach(RimefrostDragon::crumble);
    }

    /** Iron's recast window needs cast data to save; it carries nothing. */
    public static final class WindowData implements ICastDataSerializable {
        @Override
        public void reset() {
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer) {
        }

        @Override
        public void readFromBuffer(FriendlyByteBuf buffer) {
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            return new CompoundTag();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        }
    }
}
