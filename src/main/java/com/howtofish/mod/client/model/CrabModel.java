package com.howtofish.mod.client.model;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.BossFishEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A proper crab model for the boss: wide flat shell, two arms with opening
 * pincers, eight wiggling legs and eye stalks. The claws raise high while the
 * crab is leaping and droop helplessly while it is stunned.
 */
public class CrabModel extends HierarchicalModel<BossFishEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(HowToFishMod.MOD_ID, "crab_boss"), "main");

    private final ModelPart root;
    private final ModelPart shell;
    private final ModelPart rim;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftPincerTop;
    private final ModelPart leftPincerBot;
    private final ModelPart rightPincerTop;
    private final ModelPart rightPincerBot;
    private final ModelPart[] legs = new ModelPart[8];
    private final ModelPart stalkL;
    private final ModelPart stalkR;
    private final ModelPart eyeL;
    private final ModelPart eyeR;

    public CrabModel(ModelPart root) {
        this.root = root;
        this.shell = root.getChild("shell");
        this.rim = shell.getChild("rim");
        this.leftArm = shell.getChild("left_arm");
        this.rightArm = shell.getChild("right_arm");
        this.leftPincerTop = leftArm.getChild("left_pincer_top");
        this.leftPincerBot = leftArm.getChild("left_pincer_bot");
        this.rightPincerTop = rightArm.getChild("right_pincer_top");
        this.rightPincerBot = rightArm.getChild("right_pincer_bot");
        for (int i = 0; i < 8; i++) {
            this.legs[i] = shell.getChild("leg" + i);
        }
        this.stalkL = shell.getChild("stalk_l");
        this.stalkR = shell.getChild("stalk_r");
        this.eyeL = stalkL.getChild("eye_l");
        this.eyeR = stalkR.getChild("eye_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Main shell. Pivot at y=18 keeps the crab flat on the ground.
        PartDefinition shell = root.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -4.0f, -5.0f, 14.0f, 5.0f, 10.0f),
                PartPose.offset(0.0f, 19.0f, 0.0f));

        // Lower rim of the body.
        shell.addOrReplaceChild("rim",
                CubeListBuilder.create().texOffs(0, 16).addBox(-6.0f, -1.0f, -4.0f, 12.0f, 3.0f, 8.0f),
                PartPose.offset(0.0f, 0.5f, 0.0f));

        // Arms with pincers.
        PartDefinition leftArm = shell.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.0f, -2.0f, -1.0f, 4.0f, 4.0f, 6.0f),
                PartPose.offsetAndRotation(-8.0f, -2.0f, -2.0f, -0.5f, 0.4f, 0.0f));
        leftArm.addOrReplaceChild("left_pincer_top",
                CubeListBuilder.create().texOffs(0, 40).addBox(1.0f, -2.0f, -6.0f, 5.0f, 2.0f, 6.0f),
                PartPose.ZERO);
        leftArm.addOrReplaceChild("left_pincer_bot",
                CubeListBuilder.create().texOffs(0, 46).addBox(1.0f, 0.0f, -5.0f, 5.0f, 2.0f, 6.0f),
                PartPose.ZERO);

        PartDefinition rightArm = shell.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(20, 32).addBox(-3.0f, -2.0f, -1.0f, 4.0f, 4.0f, 6.0f),
                PartPose.offsetAndRotation(8.0f, -2.0f, -2.0f, -0.5f, -0.4f, 0.0f));
        rightArm.addOrReplaceChild("right_pincer_top",
                CubeListBuilder.create().texOffs(20, 40).addBox(-6.0f, -2.0f, -6.0f, 5.0f, 2.0f, 6.0f),
                PartPose.ZERO);
        rightArm.addOrReplaceChild("right_pincer_bot",
                CubeListBuilder.create().texOffs(20, 46).addBox(-6.0f, 0.0f, -5.0f, 5.0f, 2.0f, 6.0f),
                PartPose.ZERO);

        // Eight legs: 4 per side (6px long, shared UV rows).
        int[] legUvX = {0, 16, 32, 48, 0, 16, 32, 48};
        int[] legUvY = {54, 54, 54, 54, 58, 58, 58, 58};
        for (int i = 0; i < 4; i++) {
            float z = -3.6f + i * 2.4f;
            shell.addOrReplaceChild("leg" + (i * 2),
                    CubeListBuilder.create().texOffs(legUvX[i * 2], legUvY[i * 2])
                            .addBox(-6.0f, -1.0f, -1.0f, 6.0f, 2.0f, 2.0f),
                    PartPose.offsetAndRotation(-7.0f, -1.0f, z, 0.0f, 0.0f, 0.6f));
            shell.addOrReplaceChild("leg" + (i * 2 + 1),
                    CubeListBuilder.create().texOffs(legUvX[i * 2 + 1], legUvY[i * 2 + 1])
                            .addBox(0.0f, -1.0f, -1.0f, 6.0f, 2.0f, 2.0f),
                    PartPose.offsetAndRotation(7.0f, -1.0f, z, 0.0f, 0.0f, -0.6f));
        }

        // Eye stalks with eyeballs on top.
        PartDefinition stalkL = shell.addOrReplaceChild("stalk_l",
                CubeListBuilder.create().texOffs(50, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f),
                PartPose.offset(-2.5f, -4.0f, -4.6f));
        stalkL.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(50, 4).addBox(-1.0f, -2.0f, -0.5f, 2.0f, 2.0f, 1.0f),
                PartPose.offset(0.0f, -3.0f, 0.0f));
        PartDefinition stalkR = shell.addOrReplaceChild("stalk_r",
                CubeListBuilder.create().texOffs(54, 0).addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f),
                PartPose.offset(2.5f, -4.0f, -4.6f));
        stalkR.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(56, 4).addBox(-1.0f, -2.0f, -0.5f, 2.0f, 2.0f, 1.0f),
                PartPose.offset(0.0f, -3.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BossFishEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        int state = entity.getBossState();

        // Look towards the target with the whole body.
        shell.yRot = netHeadYaw * ((float) Math.PI / 180F);

        // Legs wiggle while walking, tuck in while airborne.
        boolean airborne = !entity.isOnGround();
        for (int i = 0; i < 8; i++) {
            ModelPart leg = legs[i];
            float side = (i % 2 == 0) ? 1.0f : -1.0f;
            float base = side * 0.55f;
            if (airborne) {
                leg.zRot = side * 0.15f;
                leg.yRot = 0.0f;
            } else {
                leg.zRot = base;
                leg.yRot = Mth.sin(limbSwing * 0.7f + i * 1.4f) * 0.45f * limbSwingAmount;
            }
        }

        // Claws: raised & wide while leaping, drooping while stunned, waving mildly otherwise.
        if (state == BossFishEntity.STATE_LEAPING) {
            leftArm.xRot = -1.7f;
            rightArm.xRot = -1.7f;
            leftPincerTop.xRot = -0.7f;
            rightPincerTop.xRot = -0.7f;
            leftPincerBot.xRot = 0.4f;
            rightPincerBot.xRot = 0.4f;
        } else if (state == BossFishEntity.STATE_STUNNED) {
            leftArm.xRot = 0.55f;
            rightArm.xRot = 0.55f;
            leftPincerTop.xRot = 0.0f;
            rightPincerTop.xRot = 0.0f;
            leftPincerBot.xRot = 0.0f;
            rightPincerBot.xRot = 0.0f;
        } else {
            float wave = Mth.sin(ageInTicks * 0.18f) * 0.25f;
            leftArm.xRot = -0.5f + wave;
            rightArm.xRot = -0.5f - wave;
            float snap = Mth.sin(ageInTicks * 0.5f) * 0.5f + 0.5f;
            leftPincerTop.xRot = -0.15f - snap * 0.25f;
            rightPincerTop.xRot = -0.15f - snap * 0.25f;
            leftPincerBot.xRot = 0.1f;
            rightPincerBot.xRot = 0.1f;
        }

        // Eye stalks bob and scan.
        float scan = Mth.sin(ageInTicks * 0.12f) * 0.2f;
        stalkL.yRot = scan;
        stalkR.yRot = scan;
        shell.xRot = airborne ? -0.18f : Mth.sin(ageInTicks * 0.09f) * 0.03f;
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
