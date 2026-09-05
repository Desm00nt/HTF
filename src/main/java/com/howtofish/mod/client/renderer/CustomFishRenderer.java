package com.howtofish.mod.client.renderer;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.FishModel;
import com.howtofish.mod.entity.CustomFishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CustomFishRenderer extends MobRenderer<CustomFishEntity, FishModel<CustomFishEntity>> {

    public CustomFishRenderer(EntityRendererProvider.Context context) {
        super(context, new FishModel<>(context.bakeLayer(FishModel.LAYER_LOCATION)), 0.3f);
    }

    @Override
    public ResourceLocation getTextureLocation(CustomFishEntity entity) {
        return new ResourceLocation(HowToFishMod.MOD_ID, "textures/entity/fish/" + entity.getFishType().getId() + ".png");
    }

    @Override
    public void render(CustomFishEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        float scale = entity.getFishType().getScale();
        poseStack.scale(scale, scale, scale);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, light);
        poseStack.popPose();
    }
}
