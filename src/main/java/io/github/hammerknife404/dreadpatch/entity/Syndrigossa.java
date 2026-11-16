package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Syndrigossa, the Dread Queen's undead frost wyrm: proof of concept. An Ice and Fire ice dragon
 * underneath (its model, animations, growth, flight, riding and AI all come with
 * {@link IceDragonEntity}; Ice and Fire's renderer picks the ice dragon model by {@code instanceof}),
 * one of the Dread (allied with the queen and her court, immune to Withering Rime), drawn rotted to
 * the bone by {@code SyndrigossaRenderer}. Her breath, her charge, her drops and her stats are still
 * an ice dragon's; they're next.
 */
public class Syndrigossa extends IceDragonEntity implements IDreadMob {
    /**
     * Ice and Fire looks a dragon's colour up by name for its textures, eyes and drops, and a colour
     * of our own would add itself to every ice dragon's palette (wild ones would spawn in it). So
     * underneath she's always white, and our renderer paints over it.
     */
    public static final String BASE_COLOR = "white";

    public Syndrigossa(EntityType<? extends Syndrigossa> type, Level level) {
        super(type, level);
    }

    @Override
    public String getVariant() {
        return BASE_COLOR;
    }

    /** Whom she answers to: no one yet. */
    @Nullable
    @Override
    public Entity getCommander() {
        return null;
    }

    /**
     * A dragon made from a tag without health (any {@code /summon} with NBT: it skips
     * {@code finalizeSpawn}) keeps the health it was made with, a hatchling's 20, while Ice and Fire
     * grows its max health to its age here: summoned full-grown she had 20 of 419 and fell to one
     * warden hit. Ice and Fire's own ice dragons do the same; she starts whole. A saved dragon always
     * has its health in its tag.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("Health", Tag.TAG_ANY_NUMERIC)) {
            this.setHealth(this.getMaxHealth());
        }
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
