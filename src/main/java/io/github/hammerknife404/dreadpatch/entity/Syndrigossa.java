package io.github.hammerknife404.dreadpatch.entity;

import com.iafenvoy.iceandfire.entity.IceDragonEntity;
import com.iafenvoy.iceandfire.entity.util.IDreadMob;
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
