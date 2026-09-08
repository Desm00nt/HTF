package com.howtofish.mod.client.renderer;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.client.model.OldManModel;
import com.howtofish.mod.entity.OldManEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders Old Sol, swapping to the "wide eyes / open mouth" face texture
 * whenever he is eating OR when a player nearby is holding raw fish (the
 * double take). The geometry side of the animation lives in {@link OldManModel}.
 */
public class OldManRenderer extends MobRenderer<OldManEntity, OldManModel> {
    private static final ResourceLocation NORMAL = new ResourceLocation(HowToFishMod.MOD_ID, "textures/entity/old_man.png");
    private static final ResourceLocation SURPRISED = new ResourceLocation(HowToFishMod.MOD_ID, "textures/entity/old_man_eating.png");

    public OldManRenderer(EntityRendererProvider.Context context) {
        super(context, new OldManModel(context.bakeLayer(OldManModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(OldManEntity entity) {
        return (entity.isEating() || entity.isEyesPopping()) ? SURPRISED : NORMAL;
    }
}
