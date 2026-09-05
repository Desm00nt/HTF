package com.howtofish.mod.client.renderer;

import com.howtofish.mod.block.LighthouseLampBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Renders a tall white/yellow light beam above the lamp block, reusing
 * vanilla's beacon beam shader for a proper "cool lighthouse" glow that is
 * visible for a very long distance out at sea.
 */
public class LighthouseLampRenderer implements BlockEntityRenderer<LighthouseLampBlockEntity> {

    public LighthouseLampRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LighthouseLampBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        long time = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        float[] color = new float[]{0.98f, 0.89f, 0.62f};
        BeaconRenderer.renderBeaconBeam(poseStack, buffer, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0f, time,
                0, 300, color, 0.18f, 0.03f);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
