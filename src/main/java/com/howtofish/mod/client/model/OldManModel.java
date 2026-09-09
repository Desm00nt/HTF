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
 *   <li>big nose (40,24) that SWELLS when he spots a snack,</li>
 *   <li>two real eyeball cubes (36,48) that physically bulge out of their
 *       sockets when a player walks up holding raw fish or beer,</li>
 *   <li>coat body (0,28), arms (24,28 / 38,28), legs (0,44 / 16,44).</li>
 * </ul>
 * The renderer additionally swaps to the wide-eyed face texture while the
 * eyes pop - belt and suspenders for the comedic double take.
 */
public class OldManModel extends HierarchicalModel<OldManEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(HowToFishMod.MOD_ID, "old_man"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart mouthLips;
    private final ModelPart mouthInner;
    private final ModelPart teeth;
    private final ModelPart tongue;
    private final ModelPart nose;
    private final ModelPart eyeLeft;
    private final ModelPart eyeRight;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightShin;
    private final ModelPart leftShin;

    public OldManModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.mouthLips = head.getChild("mouth_lips");
        this.mouthInner = head.getChild("mouth_inner");
        this.teeth = mouthInner.getChild("teeth");
        this.tongue = mouthInner.getChild("tongue");
        this.nose = head.getChild("nose");
        this.eyeLeft = head.getChild("eye_left");
        this.eyeRight = head.getChild("eye_right");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightShin = rightLeg.getChild("shin");
        this.leftShin = leftLeg.getChild("shin");
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

        // Salty sea-dog nose. The part pivot sits at the nose ROOT so that
        // scaling it swells the nose in place instead of flinging it up.
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(40, 24)
                        .addBox(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 1.0f),
                PartPose.offset(0.0f, -3.0f, -4.0f));

        // Real eyeballs: white 3x3 cubes with painted pupils, half sunk into
        // the face at rest and physically pushed out of the sockets on "pop".
        head.addOrReplaceChild("eye_left",
                CubeListBuilder.create().texOffs(36, 48)
                        .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f),
                PartPose.offset(2.0f, -4.5f, -2.75f));
        head.addOrReplaceChild("eye_right",
                CubeListBuilder.create().texOffs(36, 48)
                        .addBox(-1.5f, -1.5f, -1.5f, 3.0f, 3.0f, 3.0f),
                PartPose.offset(-2.0f, -4.5f, -2.5f));

        // THE REAL MOUTH (nothing painted on the face anymore): a lip ring
        // plate on the face, a dark cavity plate behind it, and teeth + a
        // tongue floating on the cavity. At rest the lips are small, flat and
        // closed; when the eyes pop - or all the while he eats - the whole
        // assembly PHYSICALLY grows and bulges out, just like the eyeballs.
        head.addOrReplaceChild("mouth_lips",
                CubeListBuilder.create().texOffs(50, 24)
                        .addBox(-2.5f, -2.0f, -0.5f, 5.0f, 4.0f, 1.0f),
                PartPose.offset(0.0f, -1.6f, -4.0f));
        PartDefinition inner = head.addOrReplaceChild("mouth_inner",
                CubeListBuilder.create().texOffs(58, 24)
                        .addBox(-2.0f, -1.5f, -0.5f, 4.0f, 3.0f, 1.0f),
                PartPose.offset(0.0f, -1.6f, -3.7f));
        inner.addOrReplaceChild("teeth",
                CubeListBuilder.create().texOffs(50, 30)
                        .addBox(-1.5f, -0.5f, -0.2f, 3.0f, 1.0f, 0.4f),
                PartPose.offset(0.0f, -0.9f, -0.6f));
        inner.addOrReplaceChild("tongue",
                CubeListBuilder.create().texOffs(56, 30)
                        .addBox(-1.0f, -0.5f, -0.2f, 2.0f, 1.0f, 0.4f),
                PartPose.offset(0.0f, 0.8f, -0.6f));

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

        // Two-segment legs: he is a seated keeper - thighs + dangling shins.
        PartDefinition rleg = root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 44)
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f),
                PartPose.offset(-2.0f, 12.0f, 0.0f));
        rleg.addOrReplaceChild("shin",
                CubeListBuilder.create().texOffs(0, 54)
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f),
                PartPose.offset(0.0f, 6.5f, 0.0f));

        PartDefinition lleg = root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 44)
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 7.0f, 4.0f),
                PartPose.offset(2.0f, 12.0f, 0.0f));
        lleg.addOrReplaceChild("shin",
                CubeListBuilder.create().texOffs(16, 54)
                        .addBox(-2.0f, 0.0f, -2.0f, 4.0f, 6.0f, 4.0f),
                PartPose.offset(0.0f, 6.5f, 0.0f));

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

        // The "eyes popping" moment: the head swells a touch AND the real
        // eyeball cubes physically push out of their sockets and grow, while
        // the nose reddens up (scales). Smoothstep so the pop snaps in.
        float bulge = pop * pop * (3.0f - 2.0f * pop);
        head.xScale = 1.0f + 0.04f * bulge;
        head.yScale = 1.0f + 0.04f * bulge;
        head.zScale = 1.0f + 0.04f * bulge;
        head.z = -0.15f * bulge;

        float eyeOut = -2.75f - 1.9f * bulge;
        float es = 1.0f + 0.55f * bulge;
        float jitter = bulge * 0.14f;
        for (ModelPart eye : new ModelPart[]{eyeLeft, eyeRight}) {
            eye.z = eyeOut;
            eye.xScale = es;
            eye.yScale = es;
            eye.zScale = es;
        }
        eyeLeft.y = -4.5f + Mth.sin(ageInTicks * 0.4f) * jitter;
        eyeRight.y = -4.5f + Mth.sin(ageInTicks * 0.4f + 2.1f) * jitter;

        float ns = 1.0f + 0.75f * bulge;
        nose.xScale = ns;
        nose.yScale = ns;
        nose.zScale = ns;

        // Arms sway idly. Legs: he sits on his stool permanently - thighs
        // forward, shins down - unless something physically shoves him, then
        // they flail for a second.
        float swing = limbSwingAmount * 0.65f;
        rightArm.xRot = -0.06f + Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * swing;
        leftArm.xRot = -0.06f + Mth.cos(limbSwing * 0.6662f) * swing;

        boolean knocked = entity.getDeltaMovement().horizontalDistanceSqr() > 0.02
                || entity.fallDistance > 0.1f;
        if (knocked) {
            // A rare shove (weaker now that he can't be hurt): legs flail,
            // body returns to its full stance for that beat.
            rightLeg.xRot = Mth.cos(limbSwing * 0.6662f + (float) Math.PI) * 0.9f - 0.5f;
            leftLeg.xRot = Mth.cos(limbSwing * 0.6662f) * 0.9f - 0.5f;
            rightShin.xRot = 0.7f;
            leftShin.xRot = 0.7f;
            body.y = 0.0f;
            head.y = 0.0f;
        } else {
            // Perpetual stool sit: thighs forward, shins down, body sunk
            // onto the seat, with a slow knee bounce so he looks alive.
            rightLeg.xRot = -1.52f;
            leftLeg.xRot = -1.57f;
            rightShin.xRot = 1.42f + Mth.sin(ageInTicks * 0.06f) * 0.04f;
            leftShin.xRot = 1.48f + Mth.sin(ageInTicks * 0.06f + 1.3f) * 0.04f;
            body.y = 1.2f;
            head.y = 1.2f;
        }

        // The mouth is CLOSED and quiet unless he is actually eating: the
        // eyes and nose keep their surprise bulge, but the lips only part on
        // a real bite. One bite = open fast, hold a beat, close, rest - a
        // readable chomp rhythm instead of a constant nervous wobble.
        float chew = 0.0f;
        if (eating) {
            float wave = Mth.sin(entity.getEatTicks() * 0.32f);   // ~1 chomp / 1.0s
            chew = wave > 0.0f ? Math.min(1.0f, wave * 1.7f) : 0.0f;
        }
        if (eating) {
            jaw.xRot = 0.9f * chew;                    // beard hinges with the bite
            rightArm.xRot = -2.15f;
            rightArm.zRot = 0.5f;
            leftArm.xRot = -0.35f;
            body.xRot = 0.05f + 0.03f * chew;
            // A small dip of the head on every swallow (the closing half).
            head.xRot += chew > 0.3f ? 0.05f : 0.11f;
        } else {
            jaw.xRot = 0.0f;                           // mouth shut when idle
            rightArm.zRot = -0.04f - 0.28f * pop; // elbows out when shocked
            leftArm.zRot = 0.04f + 0.28f * pop;
            body.xRot = 0.0f;
        }

        // ---- The 3D mouth: GROWS ONLY WHILE BITING (bite = open, close,
        //      rest). Cavity, teeth and tongue appear inside the parted lips. ----
        float mo = chew * chew * (3.0f - 2.0f * chew);  // smoothstep of the bite
        float lipsS = 1.0f + 0.75f * mo;
        mouthLips.xScale = lipsS;
        mouthLips.yScale = lipsS;
        mouthLips.zScale = 1.0f + 0.4f * mo;
        mouthLips.z = -4.0f - 0.7f * mo;                 // lips lead out a bit
        mouthLips.y = -1.6f + 0.3f * mo;                 // drop with the bite
        mouthInner.visible = chew > 0.30f;               // only inside an open mouth
        float inS = 0.8f + 0.5f * mo;
        mouthInner.xScale = inS;
        mouthInner.yScale = inS;
        mouthInner.zScale = 1.0f;
        mouthInner.z = -3.7f - 0.1f * mo;
        teeth.y = -0.9f - 0.05f * mo;                    // teeth ride the palate
        tongue.y = 0.8f + Mth.sin(ageInTicks * 1.1f) * 0.16f * mo;  // wet flick
        tongue.x = Mth.sin(ageInTicks * 0.5f) * 0.07f * mo;
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
