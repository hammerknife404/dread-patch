package io.github.hammerknife404.dreadpatch.world;

import com.iafenvoy.iceandfire.data.DragonColor;
import com.iafenvoy.iceandfire.entity.util.HomePosition;
import com.iafenvoy.iceandfire.item.block.PileBlock;
import com.iafenvoy.iceandfire.registry.IafBlocks;
import io.github.hammerknife404.dreadpatch.entity.RimefrostDragon;
import io.github.hammerknife404.dreadpatch.magic.NestRules;
import io.github.hammerknife404.dreadpatch.registry.Hoard;
import io.github.hammerknife404.dreadpatch.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

/**
 * Wild Rimefrost dragons' nests on the Glacial Plains (0.84.0, {@link NestRules}): a bowl sunk in the glacier's top,
 * heaped with hoard piles (Ice and Fire's gold, silver and copper; our iron and blacksteel), ringed with ice shards and
 * old bones, its dragon on it (undead, it never sleeps: it keeps watch). Its dragon is made Ice and Fire's way with a
 * roost's: its home the nest, never aging, whole; wild, so it hunts players (as every untamed Rimefrost dragon does) and leaves the Dread be. Taking from a hoard
 * sets its dragon on the taker, wherever they are in its reach ({@link #onBreak}).
 */
public final class RimefrostNests {
    private RimefrostNests() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RimefrostNests::onBreak);
    }

    /** A hoard pile broken by a player in the Dreadlands: any wild Rimefrost dragon whose nest it's near turns on them. */
    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Dreadlands.is(level) || !(event.getState().getBlock() instanceof PileBlock)
                || event.getPlayer().isCreative() || event.getPlayer().isSpectator()) {
            return;
        }
        Player thief = event.getPlayer();
        BlockPos at = event.getPos();
        List<RimefrostDragon> guards = level.getEntitiesOfClass(RimefrostDragon.class, new AABB(at).inflate(NestRules.GUARD_RANGE),
                dragon -> !dragon.isTame() && dragon.isAlive() && dragon.hasHomePosition && dragon.homePos != null
                        && dragon.homePos.getPosition().distSqr(at) < 24 * 24);
        for (RimefrostDragon dragon : guards) {
            dragon.setInSittingPose(false);
            dragon.setTarget(thief);
        }
    }

    /** Whether there's any blacksteel to heap (Cataclysm's, through our tag). */
    static boolean blacksteel() {
        return BuiltInRegistries.ITEM.getTag(Hoard.BLACKSTEEL_NUGGETS).map(tag -> tag.size() > 0).orElse(false);
    }

    static Block pile(NestRules.Metal metal) {
        return switch (metal) {
            case GOLD -> IafBlocks.GOLD_PILE.get();
            case SILVER -> IafBlocks.SILVER_PILE.get();
            case COPPER -> IafBlocks.COPPER_PILE.get();
            case IRON -> Hoard.IRON_PILE.get();
            case BLACKSTEEL -> Hoard.BLACKSTEEL_PILE.get();
        };
    }

    /** The nest ({@code dreadpatch:rimefrost_nest}), placed on the Glacial Plains' surface, one chunk in {@link NestRules#ONE_IN}. */
    public static final class Nest extends Feature<NoneFeatureConfiguration> {
        public Nest() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            RandomSource random = context.random();
            BlockPos origin = context.origin();
            if (origin.getY() < NestRules.MIN_Y) {
                return false;
            }
            BlockState ground = level.getBlockState(origin.below());
            if (!(ground.is(BlockTags.ICE) || ground.is(Blocks.SNOW_BLOCK) || ground.is(Blocks.SNOW))) {
                return false;  // a spire's or a village's top, or bare stone
            }
            int radius = NestRules.MIN_RADIUS + random.nextInt(NestRules.RADII);
            int low = Integer.MAX_VALUE;
            int high = Integer.MIN_VALUE;
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + (int) Math.round(Math.cos(a) * radius),
                        origin.getZ() + (int) Math.round(Math.sin(a) * radius));
                low = Math.min(low, h);
                high = Math.max(high, h);
            }
            if (high - low > NestRules.MAX_UNEVEN) {
                return false;  // the glacier's edge, or broken ice
            }
            int floor = origin.getY() - 1;  // the nest's middle's top block, before it's sunk
            boolean blacksteel = blacksteel();
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int dx = -radius - 2; dx <= radius + 2; dx++) {
                for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                    if (d <= radius) {
                        int bed = floor - NestRules.sink(d, radius);
                        // Cleared above its bed; the ice made up under it where the glacier dips.
                        for (int y = Math.max(top, bed + 1); y > bed; y--) {
                            level.setBlock(at.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                        }
                        for (int y = bed; y > Math.min(top, bed) - 1 && y > bed - 4; y--) {
                            level.setBlock(at.set(x, y, z), y == bed && random.nextInt(4) == 0 ? Blocks.BLUE_ICE.defaultBlockState()
                                    : Blocks.PACKED_ICE.defaultBlockState(), 2);
                        }
                        Block pile = pile(NestRules.metal(random.nextDouble(), blacksteel));
                        level.setBlock(at.set(x, bed + 1, z), pile.defaultBlockState()
                                .setValue(PileBlock.LAYERS, NestRules.layers(d, radius, random.nextDouble())), 2);
                    } else if (d <= radius + 1.8 && random.nextInt(3) != 0) {
                        // The rim: shards of ice, and now and then a bone, a block or two over the snow.
                        int height = 1 + random.nextInt(2);
                        for (int y = 1; y <= height; y++) {
                            BlockState shard = random.nextInt(9) == 0 ? Blocks.BONE_BLOCK.defaultBlockState()
                                    : random.nextInt(3) == 0 ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
                            level.setBlock(at.set(x, top + y, z), shard, 2);
                        }
                    }
                }
            }
            RimefrostDragon dragon = ModRegistries.RIMEFROST_DRAGON.get().create(level.getLevel());
            if (dragon == null) {
                return true;
            }
            int days = NestRules.ageDays(random.nextDouble(), random.nextDouble());
            dragon.moveTo(origin.getX() + 0.5, floor - NestRules.sink(0, radius) + 2, origin.getZ() + 0.5, random.nextInt(360), 0.0F);
            dragon.setGender(random.nextBoolean());
            dragon.growDragon(days);
            dragon.setAgingDisabled(true);
            List<DragonColor> colours = dragon.dragonType.colors();
            dragon.setVariant(colours.get(random.nextInt(colours.size())).getName());
            dragon.homePos = new HomePosition(origin, level.getLevel());
            dragon.hasHomePosition = true;
            dragon.setHunger(50);
            dragon.setPersistenceRequired();
            dragon.setHealth(dragon.getMaxHealth());
            level.addFreshEntity(dragon);
            return true;
        }
    }
}
