package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.hammerknife404.dreadpatch.entity.WhiteoutDominion;
import io.github.hammerknife404.dreadpatch.magic.WhiteoutStorm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Whiteout Dominion's snow, as vanilla draws weather but driven: in every block column of the storm
 * near the camera, sheets of vanilla's snow texture facing it, falling fast and blowing sideways
 * with the storm's wind. Round its edge, a wall of the same snow over a haze of powder snow, so it
 * shows from outside as a storm standing on the land. On the ground under it, a cover of snow
 * wherever vanilla's snow could lie, drifting in by patches and melting as the storm clears: drawn,
 * never placed, so nothing is left behind when it blows over, is unloaded, or the server stops. The
 * fog (and the flakes and wind round whoever's in it) is {@link WhiteoutClient}'s.
 * <p>
 * Drawn where vanilla draws its weather, after the water ({@link #drawStorms}), not in the entity
 * pass: vanilla draws translucent terrain after entities, so anything translucent an entity drew in
 * front of water hid it (the owner saw a lake vanish inside her storm). Nothing of it writes depth
 * (except under Fabulous, whose composite sorts its layers by the depth they write), so it's drawn back
 * to front: the snow on the ground and the wall's far side, the falling snow, then the wall's near side.
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
    /** The snow on the ground: vanilla's snow block, a sixteenth over the ground, drawn this far out, its ground looked up again this often. */
    private static final ResourceLocation COVER = ResourceLocation.withDefaultNamespace("block/snow");
    private static final float COVER_LIFT = 1.0F / 16.0F;
    private static final float COVER_SIGHT = 64.0F;
    private static final int COVER_REFRESH = 40;

    /**
     * Vanilla's entity translucent type, but drawn into the weather target (the main target, except
     * under Fabulous) and writing no depth (except under Fabulous): nothing of the storm hides what's
     * drawn after it.
     */
    private static final Function<ResourceLocation, RenderType> STORM = Util.memoize(texture -> stormType(texture, false));
    private static final Function<ResourceLocation, RenderType> STORM_FABULOUS = Util.memoize(texture -> stormType(texture, true));

    /** Storms the entity pass found in view this frame, drawn after the water ({@link #drawStorms}). */
    private static final Set<WhiteoutDominion> SEEN = Collections.newSetFromMap(new WeakHashMap<>());
    /** Where each storm's snow lies, looked up every {@link #COVER_REFRESH} ticks rather than every frame. */
    private static final Map<WhiteoutDominion, Cover> COVERS = new WeakHashMap<>();

    public WhiteoutRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    /** Only noted here (vanilla has culled it already); drawn after the water, by {@link #drawStorms}. */
    @Override
    public void render(WhiteoutDominion storm, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (storm.strength(partialTick) > 0.0F) {
            SEEN.add(storm);
        }
        super.render(storm, yaw, partialTick, pose, buffers, light);
    }

    /** Every storm in view, where vanilla draws its rain and snow: after the water, the particles and the clouds. */
    static void drawStorms(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || SEEN.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        // The camera's turn is in the model-view matrix; this is only where the storm stands from it.
        PoseStack pose = new PoseStack();
        for (WhiteoutDominion storm : List.copyOf(SEEN)) {
            if (!storm.isRemoved() && storm.level() == minecraft.level) {
                pose.pushPose();
                pose.translate(storm.getX() - camera.x, storm.getY() - camera.y, storm.getZ() - camera.z);
                draw(storm, partialTick, camera, pose.last(), buffers);
                pose.popPose();
            }
        }
        SEEN.clear();
        // Back as vanilla had it for the rest of its weather pass: the world border, and other mods.
        if (Minecraft.useShaderTransparency()) {
            RenderStateShard.WEATHER_TARGET.setupRenderState();
        } else {
            RenderSystem.depthMask(false);
        }
    }

    /**
     * One storm, back to front, each batch ended before the next (its quads sorted far to near as
     * it's drawn): the snow on the ground and the wall's far side; the falling snow; the wall's near side.
     */
    private static void draw(WhiteoutDominion storm, float partialTick, Vec3 camera, PoseStack.Pose pose, MultiBufferSource.BufferSource buffers) {
        float strength = storm.strength(partialTick);
        if (strength <= 0.0F) {
            return;
        }
        Level level = storm.level();
        float age = storm.tickCount + partialTick;
        Vec3 wind = WhiteoutClient.wind(storm);
        List<WallSegment> wall = wallNear(storm, level, camera);
        TextureAtlasSprite haze = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(HAZE);
        TextureAtlasSprite cover = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(COVER);
        RenderType blocks = stormType(InventoryMenu.BLOCK_ATLAS);
        RenderType snow = stormType(SNOW);
        VertexConsumer out = buffers.getBuffer(blocks);
        renderCover(storm, level, camera, age, pose, out, cover);
        renderWallHaze(wall, false, strength, pose, out, haze);
        buffers.endBatch(blocks);
        out = buffers.getBuffer(snow);
        renderWallSnow(wall, false, wind, age, strength, pose, out);
        renderDrifts(storm, level, camera, wind, age, strength, pose, out);
        buffers.endBatch(snow);
        out = buffers.getBuffer(blocks);
        renderWallHaze(wall, true, strength, pose, out, haze);
        buffers.endBatch(blocks);
        out = buffers.getBuffer(snow);
        renderWallSnow(wall, true, wind, age, strength, pose, out);
        buffers.endBatch(snow);
    }

    private static RenderType stormType(ResourceLocation texture) {
        return (Minecraft.useShaderTransparency() ? STORM_FABULOUS : STORM).apply(texture);
    }

    private static RenderType stormType(ResourceLocation texture, boolean fabulous) {
        return RenderType.create(fabulous ? "dreadpatch_whiteout_fabulous" : "dreadpatch_whiteout",
                DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .setOutputState(RenderStateShard.WEATHER_TARGET)
                        .setWriteMaskState(fabulous ? RenderStateShard.COLOR_DEPTH_WRITE : RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    /** Where a storm's snow can lie: block tops in it, relative to it, each with its light and the point it settles at. */
    private record Cover(int builtAt, int count, float[] x, float[] y, float[] z, float[] patch, int[] light) {
    }

    /**
     * Its snow on the ground, block by block, wherever vanilla's snow could lie (a full top, or
     * leaves; not on water or ice), drifting in and melting by patches ({@link WhiteoutStorm#settled}).
     */
    private static void renderCover(WhiteoutDominion storm, Level level, Vec3 camera, float age, PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite sprite) {
        float amount = WhiteoutStorm.cover(age, storm.getLifetime());
        if (amount <= 0.0F) {
            return;
        }
        Cover cover = COVERS.get(storm);
        if (cover == null || storm.tickCount - cover.builtAt() >= COVER_REFRESH || storm.tickCount < cover.builtAt()) {
            cover = coverOf(storm, level);
            COVERS.put(storm, cover);
        }
        double sight = COVER_SIGHT * COVER_SIGHT;
        for (int i = 0; i < cover.count(); i++) {
            double dx = storm.getX() + cover.x()[i] + 0.5 - camera.x;
            double dz = storm.getZ() + cover.z()[i] + 0.5 - camera.z;
            if (dx * dx + dz * dz > sight) {
                continue;
            }
            float alpha = WhiteoutStorm.settled(amount, cover.patch()[i]);
            if (alpha <= 0.0F) {
                continue;
            }
            float x = cover.x()[i];
            float y = cover.y()[i];
            float z = cover.z()[i];
            int lit = cover.light()[i];
            vertex(out, pose, x, y, z, sprite.getU0(), sprite.getV0(), alpha, lit);
            vertex(out, pose, x, y, z + 1.0F, sprite.getU0(), sprite.getV1(), alpha, lit);
            vertex(out, pose, x + 1.0F, y, z + 1.0F, sprite.getU1(), sprite.getV1(), alpha, lit);
            vertex(out, pose, x + 1.0F, y, z, sprite.getU1(), sprite.getV0(), alpha, lit);
        }
    }

    /** Every block top in the storm where vanilla's snow could lie, looked up now. */
    private static Cover coverOf(WhiteoutDominion storm, Level level) {
        float radius = storm.getRadius();
        int r = Mth.ceil(radius);
        int cx = Mth.floor(storm.getX());
        int cz = Mth.floor(storm.getZ());
        double floor = storm.getY() - WhiteoutStorm.DEPTH;
        double ceiling = storm.getY() + WhiteoutStorm.HEIGHT;
        int size = (2 * r + 1) * (2 * r + 1);
        float[] xs = new float[size];
        float[] ys = new float[size];
        float[] zs = new float[size];
        float[] patches = new float[size];
        int[] lights = new int[size];
        int n = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int z = cz - r; z <= cz + r; z++) {
            for (int x = cx - r; x <= cx + r; x++) {
                double dx = x + 0.5 - storm.getX();
                double dz = z + 0.5 - storm.getZ();
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (top - 1 < floor || top > ceiling) {
                    continue;
                }
                at.set(x, top, z);
                // Where a snow layer could stand: on a full top or leaves, not water, ice or another layer;
                // and not on frosted ice (vanilla's snow would lie on it), so the water it froze shows as ice.
                if (!level.getFluidState(at.below()).isEmpty() || level.getBlockState(at.below()).is(Blocks.FROSTED_ICE)
                        || !Blocks.SNOW.defaultBlockState().canSurvive(level, at)) {
                    continue;
                }
                xs[n] = (float) (x - storm.getX());
                ys[n] = (float) (top - storm.getY()) + COVER_LIFT;
                zs[n] = (float) (z - storm.getZ());
                patches[n] = WhiteoutStorm.patch(x, z);
                lights[n] = brightened(LevelRenderer.getLightColor(level, at));
                n++;
            }
        }
        return new Cover(storm.tickCount, n, xs, ys, zs, patches, lights);
    }

    /** Vanilla's snow, in each column of the storm round the camera: two sheets, at different speeds. */
    private static void renderDrifts(WhiteoutDominion storm, Level level, Vec3 camera, Vec3 wind, float age, float strength, PoseStack.Pose pose, VertexConsumer out) {
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

    /**
     * One stretch of the wall round its edge, relative to the storm: its ends, how high it stands, its
     * light, and whether it's on the near side (the camera outside it, it between the camera and the storm).
     */
    private record WallSegment(int index, float x0, float z0, float x1, float z1, float bottom, float top, int light, boolean near) {
    }

    /** Round its edge: a wall of falling snow from the ground up, fading out above. Only the part near enough the camera to matter. */
    private static void renderWallSnow(List<WallSegment> wall, boolean near, Vec3 wind, float age, float strength, PoseStack.Pose pose, VertexConsumer snow) {
        float fall = -Mth.frac(age * FALL * 1.3F);
        float blow = Mth.frac(age * BLOW);
        for (WallSegment s : wall) {
            if (s.near() != near) {
                continue;
            }
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
    }

    /** Behind the wall's snow, a haze of powder snow; a sprite can't repeat, so cells, thinning out with height. */
    private static void renderWallHaze(List<WallSegment> wall, boolean near, float strength, PoseStack.Pose pose, VertexConsumer out, TextureAtlasSprite haze) {
        for (WallSegment s : wall) {
            if (s.near() != near) {
                continue;
            }
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
        double cx = camera.x - storm.getX();
        double cz = camera.z - storm.getZ();
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
                // Its outward side faces the camera: it stands between the camera and the storm.
                float hx = (x0 + x1) / 2;
                float hz = (z0 + z1) / 2;
                boolean near = (cx - hx) * hx + (cz - hz) * hz > 0.0;
                wall.add(new WallSegment(i, x0, z0, x1, z1, bottom, top, brightened(LevelRenderer.getLightColor(level, at)), near));
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
