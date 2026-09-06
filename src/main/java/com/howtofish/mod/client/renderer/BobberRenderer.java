package com.howtofish.mod.client.renderer;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.BobberModel;
import com.howtofish.mod.entity.BobberEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Renders the fishing float plus the line from the rod tip to the float,
 * following the vanilla FishingHookRenderer approach (RenderType.leash
 * line strip with a light sag).
 */
public class BobberRenderer extends EntityRenderer<BobberEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HowToFishMod.MOD_ID, "textures/entity/bobber.png");
    private static final int SEGMENTS = 16;

    private final BobberModel model;

    public BobberRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BobberModel(context.bakeLayer(BobberModel.LAYER_LOCATION));
    }

    @Override
    public void render(BobberEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float dip = renderDip(entity, partialTicks);

        poseStack.pushPose();
        poseStack.translate(0.0f, dip, 0.0f);
        this.model.setupAnim(entity, 0.0f, 0.0f, entity.tickCount + partialTicks, 0.0f, 0.0f);
        VertexConsumer vc = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        renderLine(entity, partialTicks, poseStack, buffer, packedLight, dip);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private float renderDip(BobberEntity entity, float partialTicks) {
        return switch (entity.getState()) {
            case BobberEntity.STATE_NIBBLE -> -0.07f;
            case BobberEntity.STATE_BITE -> -0.2f;
            case BobberEntity.STATE_GROUNDED -> -0.05f;
            default -> (float) Math.sin((entity.tickCount + partialTicks) * 0.09) * 0.015f;
        };
    }

    private void renderLine(BobberEntity entity, float partialTicks, PoseStack poseStack,
                            MultiBufferSource buffer, int packedLight, float dip) {
        Entity owner = entity.getOwner();
        if (!(owner instanceof Player player)) return;

        double ox = Mth.lerp(partialTicks, player.xOld, player.getX());
        double oy = Mth.lerp(partialTicks, player.yOld, player.getY()) + player.getEyeHeight();
        double oz = Mth.lerp(partialTicks, player.zOld, player.getZ());
        float yawRad = player.getYRot() * ((float) Math.PI / 180F);
        // rod tip: right side of the player + slightly forward, below eye level
        double tipX = ox - Mth.cos(yawRad) * 0.4 - Mth.sin(yawRad) * 0.3;
        double tipY = oy - 0.15;
        double tipZ = oz - Mth.sin(yawRad) * 0.4 + Mth.cos(yawRad) * 0.3;

        double bx = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double by = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        double bz = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        float relX = (float) (tipX - bx);
        float relY = (float) (tipY - by);
        float relZ = (float) (tipZ - bz);

        poseStack.pushPose();
        VertexConsumer line = buffer.getBuffer(RenderType.leash());
        var pose = poseStack.last().pose();
        for (int i = 0; i < SEGMENTS; ++i) {
            float f0 = i / (float) SEGMENTS;
            float f1 = (i + 1) / (float) SEGMENTS;
            float sag0 = Mth.sin(f0 * (float) Math.PI) * -0.04f;
            float sag1 = Mth.sin(f1 * (float) Math.PI) * -0.04f;
            line.vertex(pose, relX * (1 - f0), relY * (1 - f0) + sag0 + dip, relZ * (1 - f0))
                    .color(20, 20, 20, 255).uv2(packedLight).endVertex();
            line.vertex(pose, relX * (1 - f1), relY * (1 - f1) + sag1 + dip, relZ * (1 - f1))
                    .color(20, 20, 20, 255).uv2(packedLight).endVertex();
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BobberEntity entity) {
        return TEXTURE;
    }
}
