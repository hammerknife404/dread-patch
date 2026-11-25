package io.github.hammerknife404.dreadpatch.entity;

import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.EventHooks;

/**
 * A dread scuttler's web shot (0.75.0, {@code ScuttlerWebs}): drawn as a cobweb, it does no harm, and where
 * it lands (at the feet of what it hits, or before the block it hits) it leaves a {@code DreadWebBlock} if
 * that spot is air and its scuttler may grief there (the {@code mobGriefing} rule, through NeoForge).
 */
public class ScuttlerWeb extends ThrowableItemProjectile {
    public ScuttlerWeb(EntityType<? extends ScuttlerWeb> type, Level level) {
        super(type, level);
    }

    public ScuttlerWeb(Level level, LivingEntity shooter) {
        super(ModRegistries.SCUTTLER_WEB.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBWEB;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        this.web(result.getEntity().blockPosition());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.web(result.getBlockPos().relative(result.getDirection()));
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    private void web(BlockPos pos) {
        if (this.level().isClientSide || !this.level().getBlockState(pos).isAir()
                || !EventHooks.canEntityGrief(this.level(), this.getOwner() != null ? this.getOwner() : this)) {
            return;
        }
        this.level().setBlockAndUpdate(pos, ModRegistries.DREAD_WEB.get().defaultBlockState());
    }
}
