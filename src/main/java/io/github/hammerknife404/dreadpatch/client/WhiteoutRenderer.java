package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.hammerknife404.dreadpatch.entity.WhiteoutDominion;
import io.github.hammerknife404.dreadpatch.magic.WhiteoutStorm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Whiteout Dominion's snow, as vanilla draws weather but driven: in every block column of the storm
 * near the camera, sheets of vanilla's snow texture facing it, falling fast and blowing sideways
 * with the storm's wind. Round its edge, a wall of the same snow over a haze of powder snow, so it
 * shows from outside as a storm standing on the land. The fog (and the flakes and wind round
 * whoever's in it) is {@link WhiteoutClient}'s.
 */
public class WhiteoutRenderer extends EntityRenderer<WhiteoutDominion> {
    private static final ResourceLocation SNOW = ResourceLocation.withDefaultNamespace("textures/environment/snow.png");
    private static final ResourceLocation HAZE = ResourceLocation.withDefaultNamespace("block/powder_snow");
    /** Its snow is a little grey: the light's poor in it. */
    private static final float SNOW_R = 0.86F;
    private static final float SNOW_G = 0.89F;
    private static final float SNOW_B = 0.93F;
    /** Texture heights (4 blocks) a tick it falls, and widths (a block) a tick it blows. */
    private static final float FALL = 0.022F;
    private static final float BLOW = 0.05F;
    /** The wall at its edge: segments this long, this tall over the ground, seen from this far. */
    private static final float WALL_SEGMENT = 2.0F;
    private static final float WALL_HEIGHT = 24.0F;
    private static final float WALL_SIGHT = 80.0F;
    private static final float HAZE_CELL = 4.0F;

    public WhiteoutRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(WhiteoutDominion storm, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float strength = storm.strength(partialTick);
        if (strength <= 0.0F) {
            return;
        }
        Level level = storm.level();
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition();
        float age = storm.tickCount + partialTick;
        Vec3 wind = WhiteoutClient.wind(storm);
        VertexConsumer snow = buffers.getBuffer(RenderType.entityTranslucent(SNOW));
        this.renderDrifts(storm, level, camera, wind, age, strength, pose.last(), snow);
        this.renderWall(storm, level, camera, wind, age, strength, pose.last(), snow, buffers);
        super.render(storm, yaw, partialTick, pose, buffers, light);
    }

    /** Vanilla's snow, in each column of the storm round the camera: two sheets, at different speeds. */
    private void renderDrifts(WhiteoutDominion storm, Level level, Vec3 camera, Vec3 wind, float age, float strength, PoseStack.Pose pose, VertexConsumer out) {
        int reach = Minecraft.useFancyGraphics() ? 10 : 6;
        int cx = Mth.floor(camera.x);
        int cy = Mth.floor(camera.y);
        int cz = Mth.floor(camera.z);
        double radius = storm.getRadius();
        double floor = storm.getY() - WhiteoutStorm.DEPTH;
        double ceiling = storm.getY() + WhiteoutStorm.HEIGHT;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int z = cz - reach; z <= cz + reach; z++) {
            for (int x = cx - reach; x <= cx + reach; x++) {
                double sx = x + 0.5 - storm.getX();
                double sz = z + 0.5 - storm.getZ();
                if (sx * sx + sz * sz > radius * radius) {
                    continue;
                }
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                float bottom = (float) Math.max(Math.max(ground, cy - reach), floor);
                float top = (float) Math.min(cy + reach, ceiling);
                if (bottom >= top) {
                    continue;
                }
                double ox = x + 0.5 - camera.x;
                double oz = z + 0.5 - camera.z;
                double far = Math.sqrt(ox * ox + oz * oz);
                if (far > reach) {
                    continue;
                }
                // Facing the camera, a block wide, as vanilla's.
                float halfX = (float) (far < 1.0E-3 ? 0.5 : -oz / far * 0.5);
                float halfZ = (float) (far < 1.0E-3 ? 0.0 : ox / far * 0.5);
                float fade = (float) (far / reach);
                float alpha = ((1.0F - fade * fade) * 0.35F + 0.55F) * strength;
                // Blown across the sheet the way the wind crosses the view.
                float across = (float) Math.signum(wind.x * halfX + wind.z * halfZ);
                at.set(x, Math.max(ground, cy), z);
                int lit = brightened(LevelRenderer.getLightColor(level, at));
                RandomSource random = RandomSource.create((long) x * x * 3121 + x * 45238971L ^ (long) z * z * 418711 + z * 13761L);
                for (int sheet = 0; sheet < 2; sheet++) {
                    float speed = (sheet == 0 ? 1.0F : 1.6F) * (0.8F + random.nextFloat() * 0.5F);
                    float u = random.nextFloat() + Mth.frac(age * BLOW * speed) * across;
                    float v = random.nextFloat() - Mth.frac(age * FALL * speed);
                    float px = (float) (x + 0.5 - storm.getX());
                    float pz = (float) (z + 0.5 - storm.getZ());
                    float y0 = (float) (bottom - storm.getY());
                    float y1 = (float) (top - storm.getY());
                    vertex(out, pose, px - halfX, y1, pz - halfZ, u, bottom * 0.25F + v, alpha, lit);
                    vertex(out, pose, px + halfX, y1, pz + halfZ, u + 1.0F, bottom * 0.25F + v, alpha, lit);
                    vertex(out, pose, px + halfX, y0, pz + halfZ, u + 1.0F, top * 0.25F + v, alpha, lit);
                    vertex(out, pose, px - halfX, y0, pz - halfZ, u, top * 0.25F + v, alpha, lit);
                }
            }
        }
    }

    /** One stretch of the wall round its edge, relative to the storm: its ends, how high it stands, its light. */
    private record WallSegment(int index, float x0, float z0, float x1, float z1, float bottom, float top, int light) {
    }

    /**
     * Round its edge: a wall of falling snow from the ground up, fading out above, with a haze of
     * powder snow behind it. Only the part near enough the camera to matter. All its snow goes first,
     * then all its haze: asking the buffers for another render type ends the last one's batch.
     */
    private void renderWall(WhiteoutDominion storm, Level level, Vec3 camera, Vec3 wind, float age, float strength, PoseStack.Pose pose, VertexConsumer snow,
                            MultiBufferSource buffers) {
        List<WallSegment> wall = wallNear(storm, level, camera);
        if (wall.isEmpty()) {
            return;
        }
        float fall = -Mth.frac(age * FALL * 1.3F);
        float blow = Mth.frac(age * BLOW);
        for (WallSegment s : wall) {
            float length = Mth.sqrt((s.x1() - s.x0()) * (s.x1() - s.x0()) + (s.z1() - s.z0()) * (s.z1() - s.z0()));
            float along = (float) Math.signum(wind.x * (s.x1() - s.x0()) + wind.z * (s.z1() - s.z0()));
            float u = s.index() * length + blow * along;
            float alpha = 0.85F * strength;
            float vBottom = (s.top() - s.bottom()) * 0.25F;
            vertex(snow, pose, s.x0(), s.top(), s.z0(), u, fall, 0.0F, s.light());
            vertex(snow, pose, s.x1(), s.top(), s.z1(), u + length, fall, 0.0F, s.light());
            vertex(snow, pose, s.x1(), s.bottom(), s.z1(), u + length, fall + vBottom, alpha, s.light());
            vertex(snow, pose, s.x0(), s.bottom(), s.z0(), u, fall + vBottom, alpha, s.light());
        }
        TextureAtlasSprite haze = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(HAZE);
        VertexConsumer out = buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        for (WallSegment s : wall) {
            // A sprite can't repeat, so cells, thinning out with height.
            float span = s.top() - s.bottom();
            int cells = Mth.ceil(span / HAZE_CELL);
            for (int c = 0; c < cells; c++) {
                float c0 = s.bottom() + span * c / cells;
                float c1 = s.bottom() + span * (c + 1) / cells;
                float alpha0 = 0.45F * strength * (1.0F - (float) c / cells);
                float alpha1 = 0.45F * strength * (1.0F - (float) (c + 1) / cells);
                vertex(out, pose, s.x0(), c1, s.z0(), haze.getU0(), haze.getV0(), alpha1, s.light());
                vertex(out, pose, s.x1(), c1, s.z1(), haze.getU1(), haze.getV0(), alpha1, s.light());
                vertex(out, pose, s.x1(), c0, s.z1(), haze.getU1(), haze.getV1(), alpha0, s.light());
                vertex(out, pose, s.x0(), c0, s.z0(), haze.getU0(), haze.getV1(), alpha0, s.light());
            }
        }
    }

    /** The stretches of its wall within {@link #WALL_SIGHT} of the camera, each standing on the ground under it. */
    private static List<WallSegment> wallNear(WhiteoutDominion storm, Level level, Vec3 camera) {
        float radius = storm.getRadius();
        int segments = Math.max(12, Mth.ceil(Mth.TWO_PI * radius / WALL_SEGMENT));
        double ceiling = storm.getY() + WhiteoutStorm.HEIGHT;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        List<WallSegment> wall = new ArrayList<>();
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            double mx = storm.getX() + (x0 + x1) / 2;
            double mz = storm.getZ() + (z0 + z1) / 2;
            double dx = mx - camera.x;
            double dz = mz - camera.z;
            if (dx * dx + dz * dz > WALL_SIGHT * WALL_SIGHT) {
                continue;
            }
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(mx), Mth.floor(mz));
            float bottom = (float) (ground - 1 - storm.getY());
            float top = (float) (Math.min(ground + WALL_HEIGHT, ceiling) - storm.getY());
            if (bottom < top) {
                at.set(Mth.floor(mx), ground, Mth.floor(mz));
                wall.add(new WallSegment(i, x0, z0, x1, z1, bottom, top, brightened(LevelRenderer.getLightColor(level, at))));
            }
        }
        return wall;
    }

    /** Snow catches what light there is, as vanilla's does: three quarters of the way to full. */
    private static int brightened(int packed) {
        int block = packed & 0xFFFF;
        int sky = packed >> 16 & 0xFFFF;
        return (block * 3 + 240) / 4 | (sky * 3 + 240) / 4 << 16;
    }

    /** Facing up for the shading, as vanilla draws the fire on a burning entity, so every sheet is lit alike. */
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v, float alpha, int light) {
        out.addVertex(pose, x, y, z)
                .setColor(SNOW_R, SNOW_G, SNOW_B, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(WhiteoutDominion storm) {
        return SNOW;
    }
}
