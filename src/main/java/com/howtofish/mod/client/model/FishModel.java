package com.howtofish.mod.client.model;

import com.howtofish.mod.HowToFishMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A generic simple 3D fish/crustacean model (body + tail fin + two side fins).
 * Every {@code FishType} reuses this geometry but with its own texture and a
 * per-type scale, which is enough to make each species look distinct in-game
 * (crabs are wide & flat, lobsters long, etc. are approximated via texture).
 */
public class FishModel<T extends net.minecraft.world.entity.Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(HowToFishMod.MOD_ID, "fish"), "main");

    private final ModelPart body;
    private final ModelPart tailFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public FishModel(ModelPart root) {
        this.body = root.getChild("body");
        this.tailFin = body.getChild("tail_fin");
        this.leftFin = body.getChild("left_fin");
        this.rightFin = body.getChild("right_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -2.5f, -6.0f, 5.0f, 5.0f, 12.0f, new CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 18.0f, 0.0f));

        body.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 18).addBox(0.0f, -2.0f, 0.0f, 0.0f, 4.0f, 5.0f),
                PartPose.offset(0.0f, 0.0f, 6.0f));

        body.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(16, 18).addBox(0.0f, 0.0f, 0.0f, 4.0f, 0.0f, 3.0f),
                PartPose.offsetAndRotation(-2.5f, 0.5f, -2.0f, 0.0f, (float) Math.toRadians(30), 0.0f));

        body.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(16, 22).addBox(-4.0f, 0.0f, 0.0f, 4.0f, 0.0f, 3.0f),
                PartPose.offsetAndRotation(2.5f, 0.5f, -2.0f, 0.0f, (float) -Math.toRadians(30), 0.0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float flop = Mth.cos(ageInTicks * 1.4f) * 0.4f;
        tailFin.yRot = flop;
        leftFin.zRot = (float) Math.toRadians(30) + Mth.cos(ageInTicks * 0.6f) * 0.15f;
        rightFin.zRot = (float) -Math.toRadians(30) - Mth.cos(ageInTicks * 0.6f) * 0.15f;
        body.yRot = Mth.cos(ageInTicks * 0.4f) * 0.08f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
