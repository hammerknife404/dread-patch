package io.github.hammerknife404.dreadpatch.block;

import io.github.hammerknife404.dreadpatch.magic.CampfireRules;
import io.github.hammerknife404.dreadpatch.registry.DreadCampfire;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What lies on a Dread Campfire (0.98.4): up to {@link CampfireRules#SLOTS} pieces of food, as on a campfire, each with
 * how long it's lain. Lit, each sublimates after {@link CampfireRules#SUBLIMATE_TICKS}: gone, with a burst of rime smoke
 * and a hiss, and {@link CampfireRules#ASH} of Ice and Fire's ash dropped. Sent to clients to be drawn as a campfire's
 * food is ({@code client/DreadCampfireRenderer}).
 */
public class DreadCampfireBlockEntity extends BlockEntity {
    private static final ResourceLocation ASH = ResourceLocation.fromNamespaceAndPath("iceandfire", "ash");
    private final NonNullList<ItemStack> items = NonNullList.withSize(CampfireRules.SLOTS, ItemStack.EMPTY);
    private final int[] lain = new int[CampfireRules.SLOTS];

    public DreadCampfireBlockEntity(BlockPos pos, BlockState state) {
        super(DreadCampfire.BLOCK_ENTITY.get(), pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    /** One of {@code stack} into its first free place (taken from the stack); whether there was one. */
    public boolean place(ItemStack stack) {
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).isEmpty()) {
                this.items.set(i, stack.split(1));
                this.lain[i] = 0;
                this.changed();
                return true;
            }
        }
        return false;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, DreadCampfireBlockEntity campfire) {
        boolean changed = false;
        for (int i = 0; i < campfire.items.size(); i++) {
            if (campfire.items.get(i).isEmpty()) {
                continue;
            }
            if (CampfireRules.sublimated(++campfire.lain[i])) {
                campfire.items.set(i, ItemStack.EMPTY);
                campfire.lain[i] = 0;
                changed = true;
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        new ItemStack(BuiltInRegistries.ITEM.get(ASH), CampfireRules.ASH));
                if (level instanceof ServerLevel server) {
                    server.sendParticles(ModRegistries.RIME_SMOKE_SMALL.get(), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.2, 0.1, 0.2, 0.02);
                    server.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.02);
                }
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.4F);
            }
        }
        if (changed) {
            campfire.changed();
        } else {
            campfire.setChanged();
        }
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
        int[] saved = tag.getIntArray("Lain");
        System.arraycopy(saved, 0, this.lain, 0, Math.min(saved.length, this.lain.length));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, true, registries);
        tag.putIntArray("Lain", this.lain);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, this.items, true, registries);
        return tag;
    }
}
