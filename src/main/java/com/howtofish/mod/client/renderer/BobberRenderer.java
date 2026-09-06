package com.howtofish.mod.client.renderer;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.BobberModel;
import com.howtofish.mod.entity.BobberEntity;
import com.howtofish.mod.entity.CustomFishEntity;
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
        // While a fish is hooked, draw a second line from the float to the fish.
        CustomFishEntity fish = entity.getSyncedFish();
        if (fish != null && entity.getState() == BobberEntity.STATE_HOOKED) {
            renderSegment(buffer, poseStack, dip,
                    Mth.lerp(partialTicks, entity.xOld, entity.getX()),
                    Mth.lerp(partialTicks, entity.yOld, entity.getY()) + dip,
                    Mth.lerp(partialTicks, entity.zOld, entity.getZ()),
                    Mth.lerp(partialTicks, fish.xOld, fish.getX()),
                    Mth.lerp(partialTicks, fish.yOld, fish.getY()) + fish.getBbHeight() * 0.6,
                    Mth.lerp(partialTicks, fish.zOld, fish.getZ()), packedLight);
        }
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
        Entity owner = entity.getSyncedOwner();
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
        renderSegment(buffer, poseStack, dip, tipX, tipY, tipZ, bx, by, bz, packedLight);
    }

    /** Draws a sagging line strip between two world positions (leash-style TRIANGLE_STRIP). */
    private void renderSegment(MultiBufferSource buffer, PoseStack poseStack, float dip,
                               double x0, double y0, double z0, double x1, double y1, double z1,
                               int packedLight) {
        float relX = (float) (x0 - x1);
        float relY = (float) (y0 - y1);
        float relZ = (float) (z0 - z1);
        float length = Mth.sqrt(relX * relX + relY * relY + relZ * relZ);
        float sag = Math.min(0.12f, length * 0.02f);

        // Horizontal perpendicular for the ribbon width.
        float hlen = Mth.sqrt(relX * relX + relZ * relZ);
        float px = hlen > 1.0e-4f ? relZ / hlen : 1.0f;
        float pz = hlen > 1.0e-4f ? -relX / hlen : 0.0f;
        float w = 0.012f;

        poseStack.pushPose();
        poseStack.translate(x1, y1, z1);
        VertexConsumer line = buffer.getBuffer(RenderType.leash());
        var pose = poseStack.last().pose();
        for (int i = 0; i <= SEGMENTS; ++i) {
            float f = i / (float) SEGMENTS;
            float sagF = Mth.sin(f * (float) Math.PI) * -sag;
            float vx = relX * f;
            float vy = relY * f + sagF;
            float vz = relZ * f;
            line.vertex(pose, vx + px * w, vy, vz + pz * w)
                    .color(15, 15, 15, 255).uv2(packedLight).endVertex();
            line.vertex(pose, vx - px * w, vy, vz - pz * w)
                    .color(15, 15, 15, 255).uv2(packedLight).endVertex();
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BobberEntity entity) {
        return TEXTURE;
    }
}
