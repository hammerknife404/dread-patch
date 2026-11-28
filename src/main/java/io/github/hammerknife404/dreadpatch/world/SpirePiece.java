package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.entity.DreadLichEntity;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import com.iafenvoy.iceandfire.registry.IafEntities;
import io.github.hammerknife404.dreadpatch.DreadPatch;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.storage.loot.LootTable;
import io.github.hammerknife404.dreadpatch.magic.SpireShape;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A spire's blocks, placed chunk by chunk from its {@link SpireShape} (rebuilt from its seed, so a
 * saved piece is only its centre and seed). Its ledge is dreadstone bricks; the air over its ledge is
 * cleared, the ground's too where the ledge starts at its foot. Half keep a loot chest on the summit
 * ({@link #SUMMIT_LOOT}: a plain chest with a loot table, so Lootr makes it per-player), a third of
 * those a Dread Lich by it, kept (0.53.0).
 */
public class SpirePiece extends StructurePiece {
    public static final ResourceKey<LootTable> SUMMIT_LOOT = ResourceKey.create(Registries.LOOT_TABLE, DreadPatch.id("chests/spire_summit"));

    private final BlockPos base;
    private final long seed;
    private final SpireShape shape;
    /**
     * Whether it may be a roost or keep ice spiders (0.98.8): spires made since, saved as {@code Roosts}. One made before
     * keeps its old roll, so none already standing (half built, or with its encounter) turns into a roost.
     */
    private final boolean roosts;

    public SpirePiece(BlockPos base, long seed) {
        super(ModRegistries.SPIRE_PIECE.get(), 0, box(base, SpireShape.roll(seed, true)));
        this.base = base;
        this.seed = seed;
        this.roosts = true;
        this.shape = SpireShape.roll(seed, true);
    }

    public SpirePiece(CompoundTag tag) {
        super(ModRegistries.SPIRE_PIECE.get(), tag);
        this.base = new BlockPos(tag.getInt("BaseX"), tag.getInt("BaseY"), tag.getInt("BaseZ"));
        this.seed = tag.getLong("Seed");
        this.roosts = tag.getBoolean("Roosts");
        this.shape = SpireShape.roll(this.seed, this.roosts);
    }

    private static BoundingBox box(BlockPos base, SpireShape shape) {
        int reach = shape.reach();
        return new BoundingBox(base.getX() - reach, base.getY() - SpireShape.FOUNDATION, base.getZ() - reach,
                base.getX() + reach, base.getY() + shape.height + 8, base.getZ() + reach);
    }

    /** Its base: the middle of its foot, on the ground. */
    public BlockPos base() {
        return this.base;
    }

    public SpireShape shape() {
        return this.shape;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("BaseX", this.base.getX());
        tag.putInt("BaseY", this.base.getY());
        tag.putInt("BaseZ", this.base.getZ());
        tag.putLong("Seed", this.seed);
        tag.putBoolean("Roosts", this.roosts);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
                            ChunkPos chunkPos, BlockPos pivot) {
        if (!this.boundingBox.intersects(chunkBox)) {
            return;
        }
        BoundingBox area = new BoundingBox(Math.max(this.boundingBox.minX(), chunkBox.minX()), Math.max(this.boundingBox.minY(), chunkBox.minY()),
                Math.max(this.boundingBox.minZ(), chunkBox.minZ()), Math.min(this.boundingBox.maxX(), chunkBox.maxX()),
                Math.min(this.boundingBox.maxY(), chunkBox.maxY()), Math.min(this.boundingBox.maxZ(), chunkBox.maxZ()));
        int reach = this.shape.reach();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = area.minX(); x <= area.maxX(); x++) {
            for (int z = area.minZ(); z <= area.maxZ(); z++) {
                int dx = x - this.base.getX();
                int dz = z - this.base.getZ();
                if (dx * dx + dz * dz > reach * reach) {
                    continue;
                }
                for (int y = area.minY(); y <= area.maxY(); y++) {
                    int dy = y - this.base.getY();
                    at.set(x, y, z);
                    BlockState state;
                    if (this.shape.ledge(dx, dy, dz)) {
                        state = IafBlocks.DREAD_STONE_BRICKS.get().defaultBlockState();
                    } else if (this.shape.headroom(dx, dy, dz)) {
                        if (level.getBlockState(at).isAir()) {
                            continue;
                        }
                        state = Blocks.AIR.defaultBlockState();
                    } else if (this.shape.solid(dx, dy, dz)) {
                        state = block(this.shape.material(dx, dy, dz));
                    } else if (this.shape.summit(dx, dy, dz) != null) {
                        state = block(this.shape.summit(dx, dy, dz));
                    } else if (this.shape.forge(dx, dy, dz) != null) {
                        state = block(this.shape.forge(dx, dy, dz));
                    } else if (this.shape.spike(dx, dy, dz)) {
                        state = block(this.shape.spikeMaterial(dx, dy, dz));
                    } else {
                        continue;
                    }
                    level.setBlock(at, state, Block.UPDATE_CLIENTS);
                }
            }
        }
        if (this.shape.roost) {
            BlockPos summit = this.base.above(this.shape.height);
            if (area.isInside(summit)) {
                this.raiseRoost(level, random, summit);
            }
        }
        for (int[] spot : this.shape.spiders()) {
            BlockPos pos = this.base.offset(spot[0], spot[1], spot[2]);
            if (area.isInside(pos)) {
                this.raiseSpider(level, pos);
            }
        }
        if (this.shape.chest) {
            int[] chest = this.shape.chestAt();
            BlockPos pos = this.base.offset(chest[0], chest[1], chest[2]);
            if (area.isInside(pos) && this.createChest(level, area, random, pos, SUMMIT_LOOT,
                    Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)) && this.shape.lich) {
                this.raiseLich(level, pos.south().east());
            }
        }
    }

    /** Its roost's dragon (0.98.8), on the summit's middle: wild, stage 3-4, its home the summit, never aging, kept. */
    private void raiseRoost(WorldGenLevel level, RandomSource random, BlockPos at) {
        io.github.hammerknife404.dreadpatch.entity.RimefrostDragon dragon = ModRegistries.RIMEFROST_DRAGON.get().create(level.getLevel());
        if (dragon == null) {
            return;
        }
        dragon.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextInt(360), 0.0F);
        dragon.setGender(random.nextBoolean());
        dragon.growDragon(SpireShape.ROOST_MIN_DAYS + random.nextInt(SpireShape.ROOST_MAX_DAYS - SpireShape.ROOST_MIN_DAYS + 1));
        dragon.setAgingDisabled(true);
        java.util.List<com.iafenvoy.iceandfire.data.DragonColor> colours = dragon.dragonType.colors();
        dragon.setVariant(colours.get(random.nextInt(colours.size())).getName());
        dragon.homePos = new com.iafenvoy.iceandfire.entity.util.HomePosition(at, level.getLevel());
        dragon.hasHomePosition = true;
        dragon.setHunger(50);
        dragon.setPersistenceRequired();
        dragon.setHealth(dragon.getMaxHealth());
        level.addFreshEntity(dragon);
    }

    /** One of its ice spiders (0.98.8), kept: risen as the world raises structures' mobs. */
    private void raiseSpider(WorldGenLevel level, BlockPos at) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ice_spider");
        if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                || !(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id).create(level.getLevel()) instanceof net.minecraft.world.entity.Mob spider)) {
            return;
        }
        spider.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextInt(360), 0.0F);
        net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(spider, level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        spider.setPersistenceRequired();
        level.addFreshEntityWithPassengers(spider);
    }

    /** Its summit's Dread Lich, by the chest, kept: risen as the world raises structures' mobs. */
    private void raiseLich(WorldGenLevel level, BlockPos at) {
        DreadLichEntity lich = IafEntities.DREAD_LICH.get().create(level.getLevel());
        if (lich == null) {
            return;
        }
        lich.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        lich.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        lich.setPersistenceRequired();
        level.addFreshEntityWithPassengers(lich);
    }

    private static BlockState block(SpireShape.Material material) {
        return switch (material) {
            case STONE -> Blocks.STONE.defaultBlockState();
            case DREAD_STONE -> IafBlocks.DREAD_STONE.get().defaultBlockState();
            case DREAD_BRICKS -> IafBlocks.DREAD_STONE_BRICKS.get().defaultBlockState();
            case DREAD_BRICKS_CRACKED -> IafBlocks.DREAD_STONE_BRICKS_CRACKED.get().defaultBlockState();
            case DREAD_BRICKS_CHISELED -> IafBlocks.DREAD_STONE_BRICKS_CHISELED.get().defaultBlockState();
            case DREAD_TILE -> IafBlocks.DREAD_STONE_TILE.get().defaultBlockState();
            case DREAD_TORCH -> IafBlocks.DREAD_TORCH.get().defaultBlockState();
            case PACKED_ICE -> Blocks.PACKED_ICE.defaultBlockState();
            case BLUE_ICE -> Blocks.BLUE_ICE.defaultBlockState();
            case SNOW -> Blocks.SNOW_BLOCK.defaultBlockState();
            case FORGE_CORE -> io.github.hammerknife404.dreadpatch.registry.DreadForge.CORE.get().defaultBlockState();
            case FORGE_BRICK -> io.github.hammerknife404.dreadpatch.registry.DreadForge.BRICK.get().defaultBlockState();
            case FORGE_INPUT -> io.github.hammerknife404.dreadpatch.registry.DreadForge.INPUT.get().defaultBlockState();
            case DRAGON_BONE -> IafBlocks.DRAGON_BONE_BLOCK.get().defaultBlockState();
        };
    }
}
