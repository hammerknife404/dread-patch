package io.github.hammerknife404.dreadpatch.block;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.iafenvoy.iceandfire.registry.IafAttributes;
import io.github.hammerknife404.dreadpatch.entity.RimefrostDragon;
import io.github.hammerknife404.dreadpatch.magic.ForgeRules;
import io.github.hammerknife404.dreadpatch.registry.DreadForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Rimefrost dragonforge's aperture (0.59.0), as Ice and Fire's {@code DragonForgeInputBlockEntity}
 * works:
 * <ul>
 * <li>a Rimefrost dragon's breath landing on it ({@link #breathedOn}, from {@code RimefrostDragon}'s
 * breath) lights the core beside it;</li>
 * <li>while its forge is whole with something to make, it draws one tame (or chained) Rimefrost dragon
 * within {@link #LURE_DISTANCE} that can see it to breathe on it ({@link #lure}: the dragon's
 * {@code burningTarget}, which {@code RimefrostDragon.updateBurnTarget} acts on). Ice and Fire looks every
 * tick; ours every {@link #LURE_EVERY}.</li>
 * </ul>
 */
public class RimefrostForgeInputBlockEntity extends BlockEntity {
    /** How far it draws a tame Rimefrost dragon from (Ice and Fire's 50, a cube). */
    public static final int LURE_DISTANCE = 50;
    static final int LURE_EVERY = 10;
    private int activeTicks;

    public RimefrostForgeInputBlockEntity(BlockPos pos, BlockState state) {
        super(DreadForge.INPUT_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RimefrostForgeInputBlockEntity aperture) {
        if (aperture.activeTicks > 0) {
            aperture.activeTicks--;
        }
        boolean active = aperture.activeTicks > 0;
        if (state.getValue(RimefrostForgeInputBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(RimefrostForgeInputBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
        if (level.getGameTime() % LURE_EVERY == 0) {
            aperture.lure();
        }
    }

    /** The forge core beside it (Ice and Fire's looks round it, not above or below). */
    @Nullable
    public static RimefrostForgeBlockEntity core(Level level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(pos.relative(side)) instanceof RimefrostForgeBlockEntity forge) {
                return forge;
            }
        }
        return null;
    }

    /** Whether its forge stands whole with something to make: a dragon breathes on it only then. */
    public boolean ready() {
        RimefrostForgeBlockEntity forge = this.level == null ? null : core(this.level, this.worldPosition);
        return forge != null && forge.ready();
    }

    /** A Rimefrost dragon's breath on it: its forge lit, its cook started by the dragon's forge speed. */
    public void breathedOn(DragonBaseEntity dragon) {
        RimefrostForgeBlockEntity forge = this.level == null ? null : core(this.level, this.worldPosition);
        if (forge == null) {
            return;
        }
        // Ours carry Ice and Fire's forge speed (DreadPatch); a dragon without it starts the cook at its default.
        AttributeInstance speed = dragon.getAttribute(IafAttributes.DRAGON_FORGE_SPEED);
        forge.breathe(speed != null ? speed.getValue() : IafAttributes.DRAGON_FORGE_SPEED.get().getDefaultValue());
        this.activeTicks = ForgeRules.LIT_TICKS;
    }

    private void lure() {
        if (this.level == null) {
            return;
        }
        boolean ready = this.ready();
        Vec3 at = Vec3.atCenterOf(this.worldPosition);
        boolean chosen = false;
        for (RimefrostDragon dragon : this.level.getEntitiesOfClass(RimefrostDragon.class, new AABB(this.worldPosition).inflate(LURE_DISTANCE))) {
            if (ready && !chosen && (dragon.isTame() || dragon.isChained()) && this.seenBy(dragon, at)) {
                dragon.burningTarget = this.worldPosition.immutable();
                chosen = true;
            } else if (this.worldPosition.equals(dragon.burningTarget)) {
                dragon.burningTarget = null;
                dragon.setBreathingFire(false);
            }
        }
    }

    /** Ice and Fire's test: a clear line from its head reaching within 10 blocks and twice its width of here. */
    private boolean seenBy(DragonBaseEntity dragon, Vec3 at) {
        Vec3 head = dragon.getHeadPosition();
        HitResult hit = this.level.clip(new ClipContext(head, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, dragon));
        return ForgeRules.seesAperture(head.distanceTo(hit.getLocation()), dragon.getBbWidth());
    }
}
