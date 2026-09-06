package com.howtofish.mod.client.model;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.OldManEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
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
 * The Old Man (lighthouse keeper) - a humanoid model with:
 * - an articulated "jaw" cube that opens while he is eating,
 * - separate GEOMETRIC eyeballs + eyebrows that physically bulge out of his
 *   face (translation + scale) whenever a player holding raw fish is nearby,
 * - a hair block and a belt for a more harmonious, "salty sea dog" look.
 */
public class OldManModel extends HierarchicalModel<OldManEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(HowToFishMod.MOD_ID, "old_man"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftBrow;
    private final ModelPart rightBrow;
    private final ModelPart hair;

    public OldManModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.leftEye = head.getChild("left_eye");
        this.rightEye = head.getChild("right_eye");
        this.leftBrow = head.getChild("left_brow");
        this.rightBrow = head.getChild("right_brow");
        this.hair = head.getChild("hair");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
                        .texOffs(0, 48).addBox(-4.5f, -9.0f, -4.5f, 9.0f, 3.0f, 9.0f, new CubeDeformation(0.2f)), // cap brim
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // Hair sticking out under the cap at the back of the head.
        head.addOrReplaceChild("hair",
                CubeListBuilder.create().texOffs(36, 48).addBox(-4.0f, -5.0f, 0.0f, 8.0f, 5.0f, 5.0f),
                PartPose.offset(0.0f, 0.0f, -0.5f));

        // Big Nose-tackle nose.
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(56, 48).addBox(-1.0f, -5.0f, -4.7f, 2.0f, 2.0f, 1.0f),
                PartPose.ZERO);

        // Jaw doubles as his beard / chin.
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -3.5f, 4.0f, 2.0f, 4.0f),
                PartPose.offset(0.0f, 2.0f, -1.0f));

        // Physical eyeballs - these BULGE OUT when fish is near.
        head.addOrReplaceChild("left_eye",
                CubeListBuilder.create().texOffs(32, 0).addBox(-1.0f, -1.0f, -0.5f, 2.0f, 2.0f, 1.0f),
                PartPose.offset(-1.8f, -5.2f, -4.4f));
        head.addOrReplaceChild("right_eye",
                CubeListBuilder.create().texOffs(32, 4).addBox(-1.0f, -1.0f, -0.5f, 2.0f, 2.0f, 1.0f),
                PartPose.offset(1.8f, -5.2f, -4.4f));

        // Bushy eyebrows that shoot UP while the eyes pop.
        head.addOrReplaceChild("left_brow",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.5f, -0.5f, -0.5f, 3.0f, 1.0f, 1.0f),
                PartPose.offset(-1.8f, -7.0f, -4.3f));
        head.addOrReplaceChild("right_brow",
                CubeListBuilder.create().texOffs(44, 2).addBox(-1.5f, -0.5f, -0.5f, 3.0f, 1.0f, 1.0f),
                PartPose.offset(1.8f, -7.0f, -4.3f));

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f),
                PartPose.offsetAndRotation(-5.0f, 2.0f, 0.0f, -0.10f, 0.0f, -0.05f));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 32).addBox(0.0f, -2.0f, -2.0f, 3.0f, 12.0f, 4.0f),
                PartPose.offsetAndRotation(5.0f, 2.0f, 0.0f, -0.10f, 0.0f, 0.05f));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(-2.0f, 12.0f, 0.0f));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 32).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(2.0f, 12.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(OldManEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.xRot = headPitch * ((float) Math.PI / 180F);
        head.yRot = netHeadYaw * ((float) Math.PI / 180F);

        rightArm.xRot = -0.10f + Mth.cos(limbSwing * 0.6662f) * 1.0f * limbSwingAmount * 0.4f;
        leftArm.xRot = -0.10f + Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.0f * limbSwingAmount * 0.4f;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.0f * limbSwingAmount * 0.5f;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 1.0f * limbSwingAmount * 0.5f;

        // ---- The bulging eyes -------------------------------------------
        float pop = entity.getEyePopAmount(ageInTicks % 1.0f);

        // Eyes push forward out of their sockets and swell up.
        float eyeZ = -4.4f - 1.5f * pop;
        float eyeScale = 1.0f + 1.3f * pop;
        leftEye.z = eyeZ;
        rightEye.z = eyeZ;
        leftEye.xScale = eyeScale;
        leftEye.yScale = eyeScale;
        leftEye.zScale = eyeScale;
        rightEye.xScale = eyeScale;
        rightEye.yScale = eyeScale;
        rightEye.zScale = eyeScale;
        // A slight nervous wobble at full pop.
        if (pop > 0.05f) {
            float wob = Mth.sin(ageInTicks * 0.9f) * 0.02f * pop;
            leftEye.x = -1.8f + wob;
            rightEye.x = 1.8f - wob;
        } else {
            leftEye.x = -1.8f;
            rightEye.x = 1.8f;
        }

        // Eyebrows shoot up with the eyes.
        float browLift = 1.6f * pop;
        leftBrow.y = -7.0f - browLift;
        rightBrow.y = -7.0f - browLift;
        leftBrow.zRot = -0.35f * pop;
        rightBrow.zRot = 0.35f * pop;
        leftBrow.z = -4.3f - 0.6f * pop;
        rightBrow.z = -4.3f - 0.6f * pop;

        if (entity.isEating()) {
            float t = Math.min(entity.getEatTicks() / 6.0f, 1.0f);
            float openClose = Mth.sin(Math.min(entity.getEatTicks(), 20) * 0.6f) * 0.5f + 0.5f;
            jaw.xRot = (float) Math.toRadians(45.0 * openClose * t);
        } else {
            jaw.xRot = 0.0f;
        }
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
