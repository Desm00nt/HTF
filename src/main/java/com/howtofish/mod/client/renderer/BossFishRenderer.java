package com.howtofish.mod.client.renderer;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.FishModel;
import com.howtofish.mod.entity.BossFishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Reuses the generic FishModel but scaled way up and with its own angrier
 * texture, since the game's bosses are simply oversized, tougher versions of
 * regular catches (e.g. the Spider Crab).
 */
public class BossFishRenderer extends MobRenderer<BossFishEntity, FishModel<BossFishEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(HowToFishMod.MOD_ID, "textures/entity/fish/spider_crab_boss.png");

    public BossFishRenderer(EntityRendererProvider.Context context) {
        super(context, new FishModel<>(context.bakeLayer(FishModel.LAYER_LOCATION)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(BossFishEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(BossFishEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.scale(2.6f, 2.6f, 2.6f);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, light);
        poseStack.popPose();
    }
}
