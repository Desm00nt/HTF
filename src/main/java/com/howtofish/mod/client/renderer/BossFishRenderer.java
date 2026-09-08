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
import net.minecraft.world.phys.Vec3;

/**
 * Boss renderer: the crab model + the special-attack telegraph - a glowing
 * RED CIRCLE on the ground under the victim that widens as the crab charges
 * its leap.
 * <p>
 * NOTE: entity renderers get a pose stack whose origin is the entity's
 * interpolated position (camera-relative). The circle used to be fed raw
 * WORLD coordinates, which placed it hundreds of blocks away - everything
 * here is converted to bobber-relative space first.
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
        renderTelegraphCircle(entity, partialTicks, poseStack, buffer, light);

        // Model scale (uniform, around the entity origin at its feet - the
        // standard 24-unit model grid keeps the legs on the ground).
        poseStack.pushPose();
        poseStack.scale(1.5f, 1.5f, 1.5f);

        // Shake violently during the telegraph.
        if (entity.getBossState() == BossFishEntity.STATE_TELEGRAPH) {
            float intensity = 0.09f * (0.4f + 0.6f * entity.getTelegraphTicks() / (float) TELEGRAPH_TIME);
            float dx = (entity.tickCount % 2 == 0 ? 1 : -1) * intensity;
            float dy = (entity.tickCount % 3 == 0 ? 1 : -1) * intensity * 0.5f;
            poseStack.translate(dx, dy, dx * 0.6f);
        }

        // Crouch slightly while charging, droop while stunned.
        if (entity.getBossState() == BossFishEntity.STATE_STUNNED) {
            poseStack.translate(0, 0.12f, 0);
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, light);
        poseStack.popPose();
    }

    /** Draws the growing red circle under the target player. */
    private void renderTelegraphCircle(BossFishEntity entity, float partialTicks, PoseStack poseStack,
                                       MultiBufferSource buffer, int light) {
        if (entity.getBossState() != BossFishEntity.STATE_TELEGRAPH) return;

        Player target = nearestPlayer(entity);
        if (target == null) return;

        float progress = 1.0f - entity.getTelegraphTicks() / (float) TELEGRAPH_TIME; // 0 -> 1
        float radius = 0.7f + 2.1f * progress;
        float pulse = 0.5f + 0.5f * Mth.sin((entity.tickCount + partialTicks) * 0.7f);
        int alphaFill = (int) (70 + 60 * pulse);
        int alphaRing = (int) (170 + 80 * progress);

        // Positions relative to the renderer's origin (the interpolated entity).
        Vec3 ep = entity.getPosition(partialTicks);
        Vec3 tp = target.getPosition(partialTicks);
        float cx = (float) (tp.x - ep.x);
        float cy = (float) (tp.y - ep.y) + 0.06f;
        float cz = (float) (tp.z - ep.z);

        poseStack.pushPose();
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        // Soft filled disc, then a bright tightening ring, then a core flash.
        drawDisc(vc, poseStack, cx, cy, cz, radius, 255, 40, 30, alphaFill);
        drawRing(vc, poseStack, cx, cy + 0.01f, cz, radius, radius + 0.22f, 255, 70, 45, alphaRing);
        drawDisc(vc, poseStack, cx, cy + 0.02f, cz, radius * 0.3f, 255, 150, 110, (int) (70 + 100 * progress));
        poseStack.popPose();
    }

    private void drawDisc(VertexConsumer vc, PoseStack poseStack, float cx, float cy, float cz,
                          float radius, int r, int g, int b, int a) {
        var pose = poseStack.last().pose();
        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2 * i / segments);
            float a1 = (float) (Math.PI * 2 * (i + 1) / segments);
            vc.vertex(pose, cx, cy, cz).color(r, g, b, a).endVertex();
            vc.vertex(pose, cx + Mth.cos(a0) * radius, cy, cz + Mth.sin(a0) * radius).color(r, g, b, a).endVertex();
            vc.vertex(pose, cx + Mth.cos(a1) * radius, cy, cz + Mth.sin(a1) * radius).color(r, g, b, a).endVertex();
        }
    }

    private void drawRing(VertexConsumer vc, PoseStack poseStack, float cx, float cy, float cz,
                          float inner, float outer, int r, int g, int b, int a) {
        var pose = poseStack.last().pose();
        int segments = 40;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2 * i / segments);
            float a1 = (float) (Math.PI * 2 * (i + 1) / segments);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            vc.vertex(pose, cx + c0 * inner, cy, cz + s0 * inner).color(r, g, b, a).endVertex();
            vc.vertex(pose, cx + c0 * outer, cy, cz + s0 * outer).color(r, g, b, a).endVertex();
            vc.vertex(pose, cx + c1 * outer, cy, cz + s1 * outer).color(r, g, b, a).endVertex();
            vc.vertex(pose, cx + c0 * inner, cy, cz + s0 * inner).color(r, g, b, a).endVertex();
            vc.vertex(pose, cx + c1 * outer, cy, cz + s1 * outer).color(r, g, b, a).endVertex();
            vc.vertex(pose, cx + c1 * inner, cy, cz + s1 * inner).color(r, g, b, a).endVertex();
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
