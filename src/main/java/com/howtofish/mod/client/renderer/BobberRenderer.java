package com.howtofish.mod.client.renderer;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.BobberModel;
import com.howtofish.mod.entity.BobberEntity;
import com.howtofish.mod.entity.CustomFishEntity;
import com.howtofish.mod.item.FishingRodCustomItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the fishing float plus the line from the ROD TIP in the player's
 * hand down to the float (vanilla FishingHookRenderer-style anchor maths),
 * and a second segment from the float to a hooked fish.
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
            // The pose stack origin is ALREADY the (interpolated) bobber position,
            // so the fish end of the line is simply (fish - bobber).
            double fx = Mth.lerp(partialTicks, fish.xOld, fish.getX());
            double fy = Mth.lerp(partialTicks, fish.yOld, fish.getY()) + fish.getBbHeight() * 0.5;
            double fz = Mth.lerp(partialTicks, fish.zOld, fish.getZ());
            renderSegment(buffer, poseStack,
                    fx - entity.getX(), fy - entity.getY() + dip, fz - entity.getZ(),
                    0.0, 0.0, 0.0, packedLight);
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

        Vec3 tip = rodTipAnchor(player, partialTicks);
        // The pose stack origin is the bobber, so everything is relative to it.
        renderSegment(buffer, poseStack,
                tip.x - entity.getX(), tip.y - entity.getY(), tip.z - entity.getZ(),
                0.0, dip, 0.0, packedLight);
    }

    /**
     * World-space position of the rod tip: derived from the holding hand the
     * same way vanilla's FishingHookRenderer derives its line anchor - eye
     * position, a lateral offset to the side of the holding arm, pushed
     * forward along the look vector and lifted/ducked by the swing arc.
     */
    public static Vec3 rodTipAnchor(Player player, float pt) {
        boolean inMain = player.getMainHandItem().getItem() instanceof FishingRodCustomItem;
        boolean inOff = player.getOffhandItem().getItem() instanceof FishingRodCustomItem;
        InteractionHand hand = inOff && !inMain ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        HumanoidArm arm = player.getMainArm();
        if (hand == InteractionHand.OFF_HAND) arm = arm.getOpposite();
        float sideSign = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;

        Vec3 eye = player.getEyePosition(pt);
        Vec3 look = player.getViewVector(pt);

        float swing = Mth.sin(Mth.clamp(player.attackAnim, 0.0f, 1.0f) * (float) Math.PI) * 0.2f;
        float walk = Mth.clamp((float) player.getDeltaMovement().horizontalDistance(), 0f, 0.2f) * 5.0f;
        float bob = Mth.sin(player.tickCount * 0.13f + player.getId() * 0.7f) * 0.018f * walk;

        if (player.isVisuallySwimming()) {
            return eye.add(look.scale(0.8)).add(0.0, -0.36, 0.0)
                    .add(new Vec3(-look.z, 0, look.x).normalize().scale(0.4 * sideSign));
        }

        // Horizontal right vector of the camera (ignore pitch for the lateral offset).
        Vec3 flat = new Vec3(-look.z, 0, look.x);
        double flatLen = Math.sqrt(flat.x * flat.x + flat.z * flat.z);
        flat = flatLen > 1.0e-4 ? flat.scale(1.0 / flatLen) : new Vec3(1, 0, 0);

        // Anchor = the projected TIP of the 3D rod model. It follows the red
        // tip of ROD_PARTS under the firstperson transform [20,0,-32]; if the
        // rod display ever changes again, tune these three constants to it.
        return eye
                .add(look.scale(0.85 + swing * 0.28))            // out to the tip
                .add(flat.scale(sideSign * (0.42 - swing * 0.22))) // along the holding arm
                .add(0.0, 0.10 + swing * 0.45 + bob, 0.0);      // raised onto the blank's line
    }

    /**
     * Draws a sagging leash-style ribbon between two points, both given
     * RELATIVE to the current pose stack origin (the bobber).
     */
    private void renderSegment(MultiBufferSource buffer, PoseStack poseStack,
                               double x0, double y0, double z0, double x1, double y1, double z1,
                               int packedLight) {
        float relX = (float) (x0 - x1);
        float relY = (float) (y0 - y1);
        float relZ = (float) (z0 - z1);
        float length = Mth.sqrt(relX * relX + relY * relY + relZ * relZ);
        float sag = Math.min(0.18f, length * 0.035f);

        // Horizontal perpendicular for the ribbon width.
        float hlen = Mth.sqrt(relX * relX + relZ * relZ);
        float px = hlen > 1.0e-4f ? relZ / hlen : 1.0f;
        float pz = hlen > 1.0e-4f ? -relX / hlen : 0.0f;
        float w = 0.035f;

        poseStack.pushPose();
        poseStack.translate((float) x1, (float) y1, (float) z1);
        VertexConsumer line = buffer.getBuffer(net.minecraft.client.renderer.RenderType.leash());
        var pose = poseStack.last().pose();
        for (int i = 0; i <= SEGMENTS; ++i) {
            float f = i / (float) SEGMENTS;
            float sagF = Mth.sin(f * (float) Math.PI) * -sag;
            float vx = relX * f;
            float vy = relY * f + sagF;
            float vz = relZ * f;
            line.vertex(pose, vx + px * w, vy, vz + pz * w)
                    .color(35, 28, 22, 255).uv2(packedLight).endVertex();
            line.vertex(pose, vx - px * w, vy, vz - pz * w)
                    .color(35, 28, 22, 255).uv2(packedLight).endVertex();
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(BobberEntity entity) {
        return TEXTURE;
    }
}
