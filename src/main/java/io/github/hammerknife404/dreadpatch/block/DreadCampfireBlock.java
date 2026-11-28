package io.github.hammerknife404.dreadpatch.block;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.mojang.serialization.MapCodec;
import io.github.hammerknife404.dreadpatch.registry.DreadCampfire;
import io.github.hammerknife404.dreadpatch.registry.DreadLights;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Dread Campfire (0.98.4; {@link io.github.hammerknife404.dreadpatch.magic.CampfireRules}): what a campfire in the
 * Dreadlands fades to, after a soul campfire (the owner's handoff). A campfire's shape, facing, lit and waterlogged; lit,
 * a dread light (6, its flames her cyan) and cold ({@code DreadCold}), and burning as rimefire does: the Dread healed,
 * anything else chilled and hurt. Food set on it (right-click, as on a campfire; any food) doesn't cook: lit, it
 * sublimates to ash ({@link DreadCampfireBlockEntity}). It burns until it's put out (0.98.5, the owner: "Dread Campfires
 * burn until put out and are immune to rain"): water in it, or a shovel, and nothing else. So it's kept out of
 * {@code #minecraft:campfires}, which other mods' rules (rain putting campfires out among them) go by, and our own dread
 * fire passes it by; flint and steel or a fire charge relight it and a shovel douses it, handled here (vanilla's tools test for its
 * own campfires), by NeoForge's item abilities.
 */
public class DreadCampfireBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<DreadCampfireBlock> CODEC = simpleCodec(DreadCampfireBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);

    public DreadCampfireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true).setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, WATERLOGGED, FACING);
    }

    /** Its light: a dread light's while lit. */
    public static int light(BlockState state) {
        return state.getValue(LIT) ? DreadLights.LIGHT : 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, water).setValue(LIT, !water)
                .setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Water put in it puts it out, as a campfire's. */
    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        if (!state.getValue(WATERLOGGED) && fluid.getType() == Fluids.WATER) {
            if (state.getValue(LIT) && !level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            level.setBlock(pos, state.setValue(WATERLOGGED, true).setValue(LIT, false), Block.UPDATE_ALL);
            level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
            return true;
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Food goes on it as on a campfire: one at a time, in its first free place. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SHOVEL_DOUSE) && state.getValue(LIT)) {
            if (!level.isClientSide) {
                level.levelEvent(null, 1009, pos, 0);
                level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, pos);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FIRESTARTER_LIGHT) && !state.getValue(LIT) && !state.getValue(WATERLOGGED)) {
            if (!level.isClientSide) {
                boolean charge = stack.is(net.minecraft.world.item.Items.FIRE_CHARGE);
                level.playSound(null, pos, charge ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                        level.getRandom().nextFloat() * 0.4F + 0.8F);
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, pos);
                if (charge) {
                    stack.consume(1, player);
                } else {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!stack.has(DataComponents.FOOD) || !(level.getBlockEntity(pos) instanceof DreadCampfireBlockEntity campfire)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (campfire.place(player.hasInfiniteMaterials() ? stack.copy() : stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.CONSUME_PARTIAL;
    }

    /** Lit, it burns as rimefire does: the Dread healed, anything else chilled and hurt. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !state.getValue(LIT) || !(entity instanceof LivingEntity living) || !living.isAlive() || living.tickCount % 20 != 0) {
            return;
        }
        if (living instanceof IDreadMob) {
            living.heal(RimefireBlock.HEAL);
        } else {
            living.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, RimefireBlock.CHILL_TICKS,
                    io.github.hammerknife404.dreadpatch.effect.RimefrostEffect.HEAVY));
            living.hurt(level.damageSources().source(ModRegistries.RIMEFIRE_DAMAGE), RimefireBlock.DAMAGE);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DreadCampfireBlockEntity campfire) {
            Containers.dropContents(level, pos, campfire.getItems());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Cold flames, its smoke rising faint, and a low crackle. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.4F + random.nextFloat() * 0.4F, 0.5F + random.nextFloat() * 0.3F, false);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ModRegistries.RIME_SMOKE_SMALL.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.03, 0.0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ModRegistries.RIME_FLAME_SMALL.get(), pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.4,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.01, 0.0);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DreadCampfireBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(LIT) ? null
                : createTickerHelper(type, DreadCampfire.BLOCK_ENTITY.get(), DreadCampfireBlockEntity::serverTick);
    }

    /** Whether {@code state} is a lit Dread Campfire (a cold dread light). */
    public static boolean litDread(BlockState state) {
        return state.getBlock() instanceof DreadCampfireBlock && state.getValue(LIT);
    }
}
