package io.github.hammerknife404.dreadpatch.block;

import com.iafenvoy.iceandfire.entity.util.IDreadMob;
import com.mojang.serialization.MapCodec;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rimefire (0.48.0): the patches of cold dread fire lightning leaves in the Dreadlands' storms. The
 * owner: "lightning ignites a rimefire patch that heals dread mobs standing in it 0.25 a second".
 * Ours: everyone else standing in it is chilled (Rimefrost) and put out (it drinks heat, as her
 * crown's dread fire does); it burns out on its own after 30-60 s. It needs solid ground under it,
 * spreads nowhere, and drops nothing. Drawn as soul fire is, in her cyan dread fire.
 * <p>0.55.0, the owner: "strictly an environmental and combat hazard for now", never harvested or lit by
 * players (no item, no loot; a punch puts it out, as vanilla fire); "a maximum light level of 6 so it will
 * not melt surrounding snow or ice" (vanilla light is white: its cyan is its flames'); non-Dread standing in
 * it take {@link #DAMAGE} a second ({@code dreadpatch:rimefire}, freezing, a Rimefrost kind) besides. Lit by
 * lightning in the Dreadlands, by a Rimefrost dragon's breath where it lands, and, {@link #BRIEF}, a single
 * block of embers for 2-4 s where a Withering Rime-Bolt strikes a block.
 */
public class RimefireBlock extends Block {
    public static final MapCodec<RimefireBlock> CODEC = simpleCodec(RimefireBlock::new);
    /** What it heals one of the Dread a second. */
    public static final float HEAL = 0.25F;
    public static final int MIN_LIFE = 600;
    public static final int MAX_LIFE = 1200;
    static final int CHILL_TICKS = 40;
    /** What it does to anyone not of the Dread, a second. */
    public static final float DAMAGE = 1.0F;
    public static final int LIGHT = 6;
    /** A bolt's embers: one block, gone in 2-4 s. */
    public static final BooleanProperty BRIEF = BooleanProperty.create("brief");
    public static final int BRIEF_MIN = 40;
    public static final int BRIEF_MAX = 80;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0);

    public RimefireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BRIEF, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BRIEF);
    }

    /**
     * A Withering Rime-Bolt's embers at {@code pos}, if it's open air rimefire can stand in: one brief block
     * and a burst of cyan sparks. Whether it took.
     */
    public static boolean embers(ServerLevel level, BlockPos pos) {
        BlockState embers = ModRegistries.RIMEFIRE.get().defaultBlockState().setValue(BRIEF, true);
        if (!level.getBlockState(pos).isAir() || !embers.canSurvive(level, pos)) {
            return false;
        }
        level.setBlock(pos, embers, Block.UPDATE_ALL);
        level.sendParticles(ModRegistries.RIME_FLAME_SMALL.get(), pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.04);
        return true;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !this.canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            int life = state.getValue(BRIEF) ? BRIEF_MIN + level.getRandom().nextInt(BRIEF_MAX - BRIEF_MIN + 1)
                    : MIN_LIFE + level.getRandom().nextInt(MAX_LIFE - MIN_LIFE + 1);
            level.scheduleTick(pos, this, life);
        }
    }

    /** Burnt out. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    /** Once a second: the Dread healed, everyone else chilled and burnt by the cold; anyone alight put out. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        if (living.isOnFire()) {
            living.clearFire();
        }
        if (living.tickCount % 20 != 0) {
            return;
        }
        if (living instanceof IDreadMob) {
            living.heal(HEAL);
        } else {
            living.addEffect(new MobEffectInstance(ModRegistries.RIMEFROST, CHILL_TICKS, io.github.hammerknife404.dreadpatch.effect.RimefrostEffect.HEAVY));
            living.hurt(level.damageSources().source(ModRegistries.RIMEFIRE_DAMAGE), DAMAGE);
        }
    }

    /** Cold flames licking up, a flake now and then, and a low crackle. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ModRegistries.RIME_FLAME_SMALL.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.2, pos.getZ() + random.nextDouble(),
                    0.0, 0.03, 0.0);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.SNOWFLAKE, pos.getX() + random.nextDouble(), pos.getY() + 0.5, pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
        if (random.nextInt(32) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    0.6F, 0.5F + random.nextFloat() * 0.2F, false);
        }
    }
}
