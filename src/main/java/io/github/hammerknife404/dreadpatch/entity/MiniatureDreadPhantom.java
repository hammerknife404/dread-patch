package io.github.hammerknife404.dreadpatch.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * A miniature dread phantom (0.63.0, the owner's dump: bats and parrots corrupted "into tiny Dread Phantom
 * variants with scaled-down stats, reusing the Dread Phantom's dual-roll bite"): a dread phantom at half
 * its size ({@code generic.scale} 0.5, which halves its box and how it's drawn), {@link #HEALTH} health
 * and a {@link #BITE} bite, with a higher voice. A bat or parrot rises as one where the Dread raise the
 * dead ({@link RisenDread}), and now and then one stirs in a cave ({@code world/RisenSpawns}).
 */
public class MiniatureDreadPhantom extends DreadPhantom {
    /** Ours: a bat's and a parrot's 6. */
    public static final double HEALTH = 6.0;
    /** Ours: a third of a dread phantom's 6. */
    public static final double BITE = 2.0;

    public MiniatureDreadPhantom(EntityType<? extends MiniatureDreadPhantom> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DreadPhantom.createAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, BITE).add(Attributes.SCALE, 0.5);
    }

    /** Vanilla sets a phantom's bite by its size whenever that's set; a miniature one keeps its own. */
    @Override
    public void setPhantomSize(int size) {
        super.setPhantomSize(size);
        AttributeInstance bite = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (bite != null) {
            bite.setBaseValue(BITE);
        }
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.5F;
    }
}
