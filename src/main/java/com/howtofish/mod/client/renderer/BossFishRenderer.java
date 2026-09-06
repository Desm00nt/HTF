package com.howtofish.mod.client.renderer;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.CrabModel;
import com.howtofish.mod.entity.BossFishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Boss renderer: the crab model + the special-attack telegraph - a glowing
 * RED CIRCLE drawn on the ground that follows the target player until the
 * crab jumps onto that spot.
 */
public class BossFishRenderer extends MobRenderer<BossFishEntity, CrabModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(HowToFishMod.MOD_ID, "textures/entity/fish/spider_crab_boss.png");

    private static final int TELEGRAPH_TIME = BossFishEntity.TELEGRAPH_TIME;

    public BossFishRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER_LOCATION)), 0.9f);
    }

    @Override
    public ResourceLocation getTextureLocation(BossFishEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BossFishEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int light) {
        // Model scale.
        poseStack.pushPose();
        poseStack.scale(1.55f, 1.55f, 1.55f);

        // Shake violently during the telegraph.
        if (entity.getBossState() == BossFishEntity.STATE_TELEGRAPH) {
            float intensity = 0.06f * (0.4f + 0.6f * entity.getTelegraphTicks() / (float) TELEGRAPH_TIME);
            float dx = (entity.tickCount % 2 == 0 ? 1 : -1) * intensity;
            float dy = (entity.tickCount % 3 == 0 ? 1 : -1) * intensity * 0.5f;
            poseStack.translate(dx, dy, dx * 0.6f);
        }

        // Droop slightly while stunned (defeated pose).
        if (entity.getBossState() == BossFishEntity.STATE_STUNNED) {
            poseStack.translate(0, 0.12f, 0);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, light);
        poseStack.popPose();

        renderTelegraphCircle(entity, partialTicks, poseStack, buffer);
    }

    /** Draws the growing red circle under the target player. */
    private void renderTelegraphCircle(BossFishEntity entity, float partialTicks, PoseStack poseStack,
                                       MultiBufferSource buffer) {
        if (entity.getBossState() != BossFishEntity.STATE_TELEGRAPH) return;

        // The circle tracks the nearest living player (mirrors the server target).
        Player target = nearestPlayer(entity);
        if (target == null) return;

        float progress = 1.0f - entity.getTelegraphTicks() / (float) TELEGRAPH_TIME; // 0 -> 1
        float radius = 0.6f + 2.2f * progress;
        float pulse = 0.5f + 0.5f * Mth.sin((entity.tickCount + partialTicks) * 0.6f);
        int alphaOuter = (int) (55 + 40 * pulse);
        int alphaInner = (int) (90 + 50 * pulse);

        double cx = target.getX();
        double cy = target.getY() + 0.08;
        double cz = target.getZ();

        poseStack.pushPose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        drawCircle(vc, poseStack, cx, cy, cz, radius, 255, 30, 30, alphaOuter);
        drawCircle(vc, poseStack, cx, cy + 0.01, cz, radius * 0.8f, 255, 60, 40, alphaInner);
        drawCircle(vc, poseStack, cx, cy + 0.02, cz, radius * 0.35f, 255, 120, 90, alphaInner);
        poseStack.popPose();
    }

    private void drawCircle(VertexConsumer vc, PoseStack poseStack, double cx, double cy, double cz,
                            float radius, int r, int g, int b, int a) {
        var pose = poseStack.last().pose();
        int segments = 26;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2 * i / segments);
            float a1 = (float) (Math.PI * 2 * (i + 1) / segments);
            float x0 = (float) (cx + Math.cos(a0) * radius);
            float z0 = (float) (cz + Math.sin(a0) * radius);
            float x1 = (float) (cx + Math.cos(a1) * radius);
            float z1 = (float) (cz + Math.sin(a1) * radius);
            vc.vertex(pose, (float) cx, (float) cy, (float) cz).color(r, g, b, a).endVertex();
            vc.vertex(pose, x0, (float) cy, z0).color(r, g, b, a).endVertex();
            vc.vertex(pose, x1, (float) cy, z1).color(r, g, b, a).endVertex();
        }
    }

    private Player nearestPlayer(BossFishEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = entity.getBoundingBox().inflate(24);
        for (Player player : mc.level.players()) {
            if (!player.isAlive() || !box.intersects(player.getBoundingBox())) continue;
            double d = entity.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = player;
            }
        }
        return best;
    }
}
