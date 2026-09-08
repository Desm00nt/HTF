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
 * The Spider Crab boss: a wide flat shell on a pair of big pincers, standing
 * on EIGHT long, two-segment spider legs (hip + shin) that reach all the way
 * down to the ground, and watching you from two stalked eyes.
 * <p>
 * Texture: {@code entity/fish/spider_crab_boss.png}, 64x128. UV map:
 * shell (0,0), rim (0,16), stalks/eyes (48/56, 0/4), claw arms (0,28)/(24,28),
 * hips rows 40..52 (L at x0, R at x22), shins rows 56..68 (L at x0, R at x28),
 * pincer tops (0,76)/(28,76), pincer bottoms (0,86)/(28,86).
 */
public class CrabModel extends HierarchicalModel<BossFishEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(HowToFishMod.MOD_ID, "crab_boss"), "main");

    /** Baseline leg angles: hips angled slightly UP-out, shins down to the ground. */
    private static final float HIP_UP = 0.25f;
    private static final float SHIN_DOWN = 1.35f;

    private final ModelPart root;
    private final ModelPart shell;
    private final ModelPart rim;
    private final ModelPart armL;
    private final ModelPart armR;
    private final ModelPart pincerLTop;
    private final ModelPart pincerLBot;
    private final ModelPart pincerRTop;
    private final ModelPart pincerRBot;
    private final ModelPart stalkL;
    private final ModelPart stalkR;
    private final ModelPart[] hips = new ModelPart[8];
    private final ModelPart[] shins = new ModelPart[8];

    public CrabModel(ModelPart root) {
        this.root = root;
        this.shell = root.getChild("shell");
        this.rim = shell.getChild("rim");
        this.armL = shell.getChild("arm_l");
        this.armR = shell.getChild("arm_r");
        this.pincerLTop = armL.getChild("pincer_l_top");
        this.pincerLBot = armL.getChild("pincer_l_bot");
        this.pincerRTop = armR.getChild("pincer_r_top");
        this.pincerRBot = armR.getChild("pincer_r_bot");
        this.stalkL = shell.getChild("stalk_l");
        this.stalkR = shell.getChild("stalk_r");
        for (int i = 0; i < 8; i++) {
            this.hips[i] = shell.getChild("hip" + i);
            this.shins[i] = this.hips[i].getChild("shin" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Shell sits high - the long legs do the rest. Ground is at y=24.
        PartDefinition shell = root.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-7.0f, -5.0f, -5.0f, 14.0f, 5.0f, 10.0f),
                PartPose.offset(0.0f, 15.0f, 0.0f));

        shell.addOrReplaceChild("rim",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-5.5f, -1.0f, -4.0f, 11.0f, 3.0f, 8.0f),
                PartPose.ZERO);

        // Claw arms out of the front sides, long pincers at their ends.
        PartDefinition armL = shell.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(0, 28)
                        .addBox(-6.0f, -2.0f, -2.0f, 6.0f, 4.0f, 4.0f),
                PartPose.offsetAndRotation(-7.0f, -2.0f, -4.0f, 0.0f, -0.55f, 0.15f));
        armL.addOrReplaceChild("pincer_l_top",
                CubeListBuilder.create().texOffs(0, 76)
                        .addBox(-6.0f, -2.5f, -3.0f, 6.0f, 3.0f, 6.0f),
                PartPose.offset(-6.0f, 0.0f, 0.0f));
        armL.addOrReplaceChild("pincer_l_bot",
                CubeListBuilder.create().texOffs(0, 86)
                        .addBox(-6.0f, 0.0f, -3.0f, 6.0f, 2.0f, 6.0f),
                PartPose.offset(-6.0f, 0.0f, 0.0f));

        PartDefinition armR = shell.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(24, 28)
                        .addBox(0.0f, -2.0f, -2.0f, 6.0f, 4.0f, 4.0f),
                PartPose.offsetAndRotation(7.0f, -2.0f, -4.0f, 0.0f, 0.55f, -0.15f));
        armR.addOrReplaceChild("pincer_r_top",
                CubeListBuilder.create().texOffs(28, 76)
                        .addBox(0.0f, -2.5f, -3.0f, 6.0f, 3.0f, 6.0f),
                PartPose.offset(6.0f, 0.0f, 0.0f));
        armR.addOrReplaceChild("pincer_r_bot",
                CubeListBuilder.create().texOffs(28, 86)
                        .addBox(0.0f, 0.0f, -3.0f, 6.0f, 2.0f, 6.0f),
                PartPose.offset(6.0f, 0.0f, 0.0f));

        // EIGHT long spider legs: hip angled up-out, shin reaching down to the
        // ground. Pairs along the shell's flanks: z = -3.6 .. +3.6.
        int[] legHipUvX = {0, 22, 0, 22, 0, 22, 0, 22};
        int[] legHipUvY = {40, 40, 44, 44, 48, 48, 52, 52};
        int[] legShinUvX = {0, 28, 0, 28, 0, 28, 0, 28};
        int[] legShinUvY = {56, 56, 60, 60, 64, 64, 68, 68};
        for (int i = 0; i < 8; i++) {
            boolean left = i % 2 == 0;
            float z = -3.6f + (i / 2) * 2.4f;
            PartDefinition hip = shell.addOrReplaceChild("hip" + i,
                    CubeListBuilder.create().texOffs(legHipUvX[i], legHipUvY[i])
                            .addBox(left ? -8.0f : 0.0f, -1.0f, -1.0f, 8.0f, 2.0f, 2.0f),
                    PartPose.offsetAndRotation(left ? -7.0f : 7.0f, 0.0f, z,
                            0.0f, 0.0f, left ? HIP_UP : -HIP_UP));
            hip.addOrReplaceChild("shin" + i,
                    CubeListBuilder.create().texOffs(legShinUvX[i], legShinUvY[i])
                            .addBox(left ? -11.0f : 0.0f, -1.0f, -1.0f, 11.0f, 2.0f, 2.0f),
                    PartPose.offsetAndRotation(left ? -8.0f : 8.0f, 0.0f, 0.0f,
                            0.0f, 0.0f, left ? -SHIN_DOWN : SHIN_DOWN));
        }

        // Eye stalks poking over the front rim.
        PartDefinition stalkL = shell.addOrReplaceChild("stalk_l",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f),
                PartPose.offset(-2.4f, -5.0f, -4.2f));
        stalkL.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(48, 4)
                        .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.offset(0.0f, -3.0f, 0.0f));
        PartDefinition stalkR = shell.addOrReplaceChild("stalk_r",
                CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-0.5f, -3.0f, -0.5f, 1.0f, 3.0f, 1.0f),
                PartPose.offset(2.4f, -5.0f, -4.2f));
        stalkR.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(56, 4)
                        .addBox(-1.0f, -2.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.offset(0.0f, -3.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 128);
    }

    @Override
    public void setupAnim(BossFishEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        int state = entity.getBossState();
        boolean airborne = !entity.isOnGround();
        float amt = Mth.clamp(limbSwingAmount, 0.0f, 1.0f);

        // Body yaw is applied by the renderer; add only a salty roll/bob.
        shell.zRot = Mth.sin(ageInTicks * 0.16f) * 0.035f;
        shell.y = 15.0f + Mth.sin(ageInTicks * 0.19f) * 0.35f * (airborne ? 0.3f : 1.0f);

        for (int i = 0; i < 8; i++) {
            boolean left = i % 2 == 0;
            int pair = i / 2;
            ModelPart hip = hips[i];
            ModelPart shin = shins[i];
            // Tripod-ish gait: alternate diagonal pairs, extra phase per row.
            float phase = limbSwing * 1.7f + pair * 1.2f + (left ? 0.0f : (float) Math.PI);
            float flap = Mth.sin(phase) * 0.34f * amt;
            float knee = Mth.sin(phase + 1.1f) * 0.22f * amt;

            switch (state) {
                case BossFishEntity.STATE_STUNNED -> {
                    // Limp: every leg collapses DOWN and slightly out - no
                    // raised tips any more, even if he is jolted airborne the
                    // frame the player lands on him.
                    hip.zRot = (left ? -0.30f : 0.30f) + Mth.sin(ageInTicks * 0.6f + i) * 0.04f;
                    shin.zRot = (left ? -1.05f : 1.05f);
                    hip.yRot = (i % 3 - 1) * 0.18f;   // random splay fore/aft
                    shin.yRot = 0.0f;
                }
                case BossFishEntity.STATE_LEAPING -> {
                    // Tucked in mid-jump.
                    hip.zRot = (left ? 0.72f : -0.72f);
                    shin.zRot = (left ? -2.3f : 2.3f);
                    hip.yRot = 0.0f;
                    shin.yRot = 0.0f;
                }
                case BossFishEntity.STATE_TELEGRAPH -> {
                    // Coiled LOW like a spring: knees bent deep, tips pressed
                    // into the sand - then the whole mass launches from here.
                    hip.zRot = (left ? -0.12f : 0.12f) + Mth.sin(ageInTicks * 2.2f + i) * 0.04f;
                    shin.zRot = (left ? -1.25f : 1.25f);
                    hip.yRot = 0.0f;
                    shin.yRot = 0.0f;
                }
                default -> {
                    if (airborne) {
                        hip.zRot = (left ? 0.55f : -0.55f) + flap * 0.4f;
                        shin.zRot = (left ? -1.0f : 1.0f) + knee * 0.3f;
                        hip.yRot = 0.0f;
                        shin.yRot = 0.0f;
                    } else {
                        hip.zRot = (left ? HIP_UP : -HIP_UP) + flap;
                        shin.zRot = (left ? -SHIN_DOWN : SHIN_DOWN) - knee;
                        // Shins also steer fore/aft a bit while scuttling.
                        hip.yRot = Mth.sin(phase + 0.6f) * 0.3f * amt;
                        shin.yRot = -hip.yRot * 0.5f;
                    }
                }
            }
        }

        // Claws: raise & snap during telegraph, swing while running, droop on stun.
        float snap = Mth.sin(ageInTicks * 0.55f) * 0.5f + 0.5f;
        armL.yRot = -0.55f;
        armR.yRot = 0.55f;
        switch (state) {
            case BossFishEntity.STATE_TELEGRAPH -> {
                armL.xRot = -1.5f;
                armR.xRot = -1.5f;
                armL.zRot = 0.35f + Mth.sin(ageInTicks * 1.8f) * 0.08f;
                armR.zRot = -0.35f - Mth.sin(ageInTicks * 1.8f) * 0.08f;
                openPincers(0.75f + snap * 0.25f);
            }
            case BossFishEntity.STATE_LEAPING -> {
                armL.xRot = -1.9f;
                armR.xRot = -1.9f;
                armL.zRot = 0.5f;
                armR.zRot = -0.5f;
                openPincers(1.0f);
            }
            case BossFishEntity.STATE_STUNNED -> {
                // Limp: the claw arms hang DOWN and the pincers gape slack,
                // gently swaying - the punish window, no raised "victory" arms.
                float droop = Mth.sin(ageInTicks * 0.5f) * 0.06f;
                armL.xRot = -0.95f + droop;
                armR.xRot = -0.95f - droop;
                armL.zRot = -0.12f;
                armR.zRot = 0.12f;
                openPincers(0.45f + droop * 2.0f);
            }
            default -> {
                float reach = 0.45f * amt;
                armL.xRot = -0.15f - reach + Mth.sin(ageInTicks * 0.3f) * 0.08f;
                armR.xRot = -0.15f - reach - Mth.sin(ageInTicks * 0.3f) * 0.08f;
                armL.yRot = -0.55f;
                armR.yRot = 0.55f;
                openPincers(0.12f + snap * 0.1f);
            }
        }

        // Eye stalks: bob and scan; rattle violently during the telegraph.
        float scan = Mth.sin(ageInTicks * 0.13f) * 0.22f;
        float shake = state == BossFishEntity.STATE_TELEGRAPH ? Mth.sin(ageInTicks * 1.9f) * 0.12f : 0.0f;
        stalkL.yRot = scan + shake;
        stalkR.yRot = scan - shake;
        stalkL.xRot = state == BossFishEntity.STATE_TELEGRAPH ? -0.25f : 0.0f;
        stalkR.xRot = stalkL.xRot;
    }

    /** Opens the pincer halves around the natural hinge axis (zRot of the
     *  horizontal claw boxes: left top up = +, mirrored on the right). */
    private void openPincers(float open) {
        pincerLTop.zRot = open * 0.42f;
        pincerRTop.zRot = -open * 0.42f;
        pincerLBot.zRot = -open * 0.30f;
        pincerRBot.zRot = open * 0.30f;
        pincerLTop.xRot = 0.0f;
        pincerRTop.xRot = 0.0f;
        pincerLBot.xRot = 0.0f;
        pincerRBot.xRot = 0.0f;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
