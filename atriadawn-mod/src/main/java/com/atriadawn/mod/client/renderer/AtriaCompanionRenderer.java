package com.atriadawn.mod.client.renderer;

import com.atriadawn.mod.entity.AtriaCompanionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Рендер компаньона: стандартная гуманоидная модель + собственная текстура. */
public class AtriaCompanionRenderer extends HumanoidMobRenderer<AtriaCompanionEntity, HumanoidModel<AtriaCompanionEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("atriadawn", "textures/entity/atria_companion.png");

    public AtriaCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AtriaCompanionEntity entity) {
        return TEXTURE;
    }
}
