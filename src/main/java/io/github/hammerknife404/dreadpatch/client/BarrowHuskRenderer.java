package io.github.hammerknife404.dreadpatch.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.hammerknife404.dreadpatch.entity.BarrowHusk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Barrow Husk: its caster's shape in rime-glass, iced blue and half clear, standing still where they
 * stood. A player's is their own model (slim or wide arms) in their skin, with the skin layers they
 * show, at a player's size; anyone else's is hers, her eyes still lit. It fades over its last half
 * second.
 */
public final class BarrowHuskRenderer extends EntityRenderer<BarrowHusk> {
    /** The colours iced over, and how clear the glass is (alpha, of 255). */
    private static final int ICE_R = 175;
    private static final int ICE_G = 205;
    private static final int ICE_B = 255;
    private static final int ALPHA = 165;
    private static final int FADE_TICKS = 10;
    /** As vanilla's PlayerRenderer draws a player. */
    private static final float PLAYER_SCALE = 0.9375F;

    private final DreadQueenModel<BarrowHusk> queen;
    private final PlayerModel<BarrowHusk> wide;
    private final PlayerModel<BarrowHusk> slim;

    public BarrowHuskRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.queen = new DreadQueenModel<>(context.bakeLayer(DreadQueenModel.LAYER));
        this.wide = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(BarrowHusk husk, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float left = BarrowHusk.LIFETIME - (husk.tickCount + partialTick);
        float fade = Mth.clamp(left / FADE_TICKS, 0.0F, 1.0F);
        int glass = FastColor.ARGB32.color(Math.round(ALPHA * fade), ICE_R, ICE_G, ICE_B);
        pose.pushPose();
        // As LivingEntityRenderer places a humanoid: turned to its body's facing, flipped, scaled, lifted.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - husk.getYRot()));
        pose.scale(-1.0F, -1.0F, 1.0F);
        if (husk.isPlayerShaped()) {
            UUID caster = husk.getOwnerId();
            PlayerSkin skin = skin(caster != null ? caster : husk.getUUID());
            PlayerModel<BarrowHusk> model = skin.model() == PlayerSkin.Model.SLIM ? this.slim : this.wide;
            showLayers(model, caster != null ? husk.level().getPlayerByUUID(caster) : null);
            pose.scale(PLAYER_SCALE, PLAYER_SCALE, PLAYER_SCALE);
            pose.translate(0.0F, -1.501F, 0.0F);
            stand(model, husk);
            model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(skin.texture())), light, OverlayTexture.NO_OVERLAY, glass);
        } else {
            pose.translate(0.0F, -1.501F, 0.0F);
            stand(this.queen, husk);
            this.queen.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(DreadQueenRenderer.TEXTURE)), light, OverlayTexture.NO_OVERLAY, glass);
            int eyes = FastColor.ARGB32.color(Math.round(255 * fade), 255, 255, 255);
            this.queen.renderToBuffer(pose, buffers.getBuffer(DreadQueenRenderer.EYES), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, eyes);
        }
        pose.popPose();
        super.render(husk, entityYaw, partialTick, pose, buffers, light);
    }

    /**
     * Standing still, grown. A model is made {@code young} (a baby's half-size body) until a
     * renderer says otherwise, as LivingEntityRenderer does from {@code isBaby()} each frame: until
     * 0.32.1 nothing did here, so every husk stood half her size.
     */
    private static void stand(HumanoidModel<BarrowHusk> model, BarrowHusk husk) {
        model.young = false;
        model.riding = false;
        model.crouching = false;
        model.attackTime = 0.0F;
        model.setupAnim(husk, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    /** The caster's skin as the tab list has it (so even out of sight), else the default skin for their UUID. */
    private static PlayerSkin skin(UUID caster) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection != null ? connection.getPlayerInfo(caster) : null;
        return info != null ? info.getSkin() : DefaultPlayerSkin.get(caster);
    }

    /** The skin layers the caster shows (hat, jacket, sleeves, trousers), as PlayerRenderer does; all of them if they're out of sight. */
    private static void showLayers(PlayerModel<BarrowHusk> model, @Nullable Player caster) {
        model.setAllVisible(true);
        if (caster != null) {
            model.hat.visible = caster.isModelPartShown(PlayerModelPart.HAT);
            model.jacket.visible = caster.isModelPartShown(PlayerModelPart.JACKET);
            model.leftPants.visible = caster.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
            model.rightPants.visible = caster.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
            model.leftSleeve.visible = caster.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            model.rightSleeve.visible = caster.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BarrowHusk husk) {
        return DreadQueenRenderer.TEXTURE;
    }
}
