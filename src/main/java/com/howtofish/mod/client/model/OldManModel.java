package com.howtofish.mod.client.model;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.OldManEntity;
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
 * Old Sol, the lighthouse keeper - a clean humanoid built on the standard
 * 64x64 skin layout so it matches the hand-painted texture exactly:
 * <ul>
 *   <li>head (0,0) 8x8x8, with a wide-brim sailor hat (crown 32,0 / brim 0,16),</li>
 *   <li>beard-as-jaw (40,16) that hinges open while he eats,</li>
 *   <li>big nose (40,24), coat body (0,28), arms (24,28 / 38,28), legs (0,44 / 16,44).</li>
 * </ul>
 * The "bulging eyes" moment is now tasteful: when a player walks up holding
 * raw fish his whole head swells slightly, tilts toward the fish and the
 * renderer swaps to the wide-eyed face texture - no floating eyeball cubes.
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

    public OldManModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // Sailor hat: wide brim + slightly taller crown, painted dark navy.
        head.addOrReplaceChild("hat",
                CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-4.0f, -11.0f, -4.0f, 8.0f, 3.0f, 8.0f)
                        .texOffs(0, 16).addBox(-5.0f, -8.6f, -5.0f, 10.0f, 1.0f, 10.0f),
                PartPose.ZERO);

        // Beard doubles as the hinged jaw (rotates at the mouth line).
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-3.0f, 0.0f, -4.0f, 6.0f, 4.0f, 4.0f),
                PartPose.offset(0.0f, -1.5f, -0.5f));

        // Salty sea-dog nose.
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(40, 24)
                        .addBox(-1.0f, -4.0f, -5.0f, 2.0f, 2.0f, 1.0f),
                PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 28)
                        .addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(24, 28)
                        .addBox(-3.0f, -1.0f, -2.0f, 3.0f, 12.0f, 4.0f),
                PartPose.offsetAndRotation(-4.0f, 1.0f, 0.0f, -0.06f, 0.0f, -0.04f));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(38, 28)
                        .addBox(0.0f, -1.0f, -2.0f, 3.0f, 12.0f, 4.0f),
                PartPose.offsetAndRotation(4.0f, 1.0f, 0.0f, -0.06f, 0.0f, 0.04f));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 44)
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(-2.0f, 12.0f, 0.0f));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 44)
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
                PartPose.offset(2.0f, 12.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(OldManEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float pop = Mth.clamp(entity.eyePopAmount, 0.0f, 1.0f);
        boolean eating = entity.isEating();

        // Head follows the player, dips a bit when excited, tilts UP at full pop
        // (leaning back to look at the fish) and gently breathes when idle.
        head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        head.xRot = headPitch * ((float) Math.PI / 180F) * 0.85f
                + Mth.sin(ageInTicks * 0.045f) * 0.025f
                - 0.22f * pop
                + (eating ? -0.08f : 0.0f);

        // The "eyes popping" moment: a subtle whole-head swell instead of
        // floating eyeballs - it reads as a comical double take.
        float s = 1.0f + 0.06f * pop;
        head.xScale = s;
        head.yScale = s;
        head.zScale = s;
        head.z = -0.15f * pop;

        // Walking swing of arms & legs (eaten pose overrides the right arm).
        float swing = limbSwingAmount * 0.65f;
        rightArm.xRot = -0.06f + Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * swing;
        leftArm.xRot = -0.06f + Mth.cos(limbSwing * 0.6662f) * swing;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662f) * limbSwingAmount;

        if (eating) {
            // Chomp: jaw hinges open and shut on a timer, hand to mouth.
            float chew = Mth.sin(entity.getEatTicks() * 0.75f) * 0.5f + 0.5f;
            jaw.xRot = 0.18f + 0.62f * chew;
            rightArm.xRot = -2.15f;
            rightArm.zRot = 0.35f;
            leftArm.xRot = -0.35f;
            body.xRot = 0.06f;
        } else {
            jaw.xRot = 0.0f;
            rightArm.zRot = -0.04f - 0.28f * pop; // elbows out when shocked
            leftArm.zRot = 0.04f + 0.28f * pop;
            body.xRot = 0.0f;
        }
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
