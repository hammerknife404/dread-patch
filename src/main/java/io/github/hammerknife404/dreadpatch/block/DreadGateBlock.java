package io.github.hammerknife404.dreadpatch.block;

import com.mojang.serialization.MapCodec;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import com.iafenvoy.iceandfire.entity.DreadMobEntity;
import io.github.hammerknife404.dreadpatch.entity.DreadPatrols;
import io.github.hammerknife404.dreadpatch.entity.DreadGate;
import io.github.hammerknife404.dreadpatch.magic.GateTiming;
import io.github.hammerknife404.dreadpatch.world.Dreadlands;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The open face of a Dread Gate while its ritual runs ({@link DreadGate}): a thin pane like a Nether
 * portal's, filling the frame, lit, with nothing to walk into and nothing to drop. As a Nether
 * portal's does, it falls apart when its frame is broken (any neighbour in its own plane that's
 * neither gate nor frame), and a pane whose ritual is gone (killed, or lost some other way) clears
 * itself on a random tick. Outside the Dreadlands an open one now and then lets one of the Dread out (0.76.0).
 */
public class DreadGateBlock extends Block {
    public static final MapCodec<DreadGateBlock> CODEC = simpleCodec(DreadGateBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** What a gate is framed with: Ice and Fire's dreadstone. */
    public static final TagKey<Block> FRAME = TagKey.create(Registries.BLOCK, DreadPatch.id("dread_gate_frame"));
    private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);
    /** How far from a pane its ritual may stand (a gate is at most 5 by 7). */
    private static final double RITUAL_REACH = 8.0;

    public DreadGateBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean inPlane = direction.getAxis() == state.getValue(AXIS) || direction.getAxis().isVertical();
        return inPlane && !neighbor.is(this) && !neighbor.is(FRAME) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getEntitiesOfClass(DreadGate.class, new AABB(pos).inflate(RITUAL_REACH)).isEmpty()) {
            level.removeBlock(pos, false);
            return;
        }
        if (!Dreadlands.is(level) && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                && GateTiming.spawnsDread(random.nextInt(GateTiming.SPAWN_ROLL), level.getDifficulty().getId())) {
            spawnDread(state, level, pos, random);
        }
    }

    /**
     * One of the Dread stepping out of the gate (0.76.0, the owner: "random dread mob spawns from an active dreadlands
     * portal, about double the chance nether portals do for pigmen"): as vanilla's Nether portal brings a zombified
     * piglin, down through the panes to the frame and onto it, facing out of either face. A random member of her court
     * (thrall, ghoul, knight or scuttler, by its weights); none if it hasn't room.
     */
    private static void spawnDread(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos foot = pos;
        while (level.getBlockState(foot.below()).is(state.getBlock())) {
            foot = foot.below();
        }
        if (!level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP)) {
            return;
        }
        DreadMobEntity mob = DreadPatrols.loose(level, random);
        if (mob == null) {
            return;
        }
        float facing = (state.getValue(AXIS) == Direction.Axis.X ? 0.0F : 90.0F) + (random.nextBoolean() ? 180.0F : 0.0F);
        mob.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, facing, 0.0F);
        mob.setYHeadRot(facing);
        if (!level.noCollision(mob, mob.getBoundingBox().deflate(0.01))) {
            return;
        }
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(foot), MobSpawnType.NATURAL, null);
        if (level.addFreshEntity(mob)) {
            Entity mount = mob.getVehicle();
            if (mount != null) {
                mob.stopRiding();
                mount.discard();
            }
        }
    }

    /** Cold dread fire and snow drifting off both faces. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean alongX = state.getValue(AXIS) == Direction.Axis.X;
        for (int i = 0; i < 2; i++) {
            double out = (random.nextBoolean() ? 1 : -1) * (0.02 + random.nextDouble() * 0.04);
            double x = pos.getX() + (alongX ? random.nextDouble() : 0.5);
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + (alongX ? 0.5 : random.nextDouble());
            level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SNOWFLAKE : ModRegistries.RIME_FLAME_SMALL.get(),
                    x, y, z, alongX ? 0.0 : out, 0.01, alongX ? out : 0.0);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> state;
        };
    }
}
