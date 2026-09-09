package com.howtofish.mod.client.model;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.CustomFishEntity;
import com.howtofish.mod.entity.FishType;
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
 * One 64x64 rigged model, SIX genuinely different animals: per species the
 * relevant parts are shown / hidden / rescaled and animated differently.
 * Anchovy is a slim darter with a snout, herring carries a dorsal + forked
 * tail, the shrimp bends with feelers and walks on little legs, the crab is a
 * flat shell on six legs, the puffer is a spiny balloon and the lobster is a
 * long armoured crawler with a tail fan and antennae.
 */
public class FishModel<T extends net.minecraft.world.entity.Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(HowToFishMod.MOD_ID, "fish"), "main");

    private final ModelPart body;
    private final ModelPart tailFin;
    private final ModelPart tailFan;
    private final ModelPart dorsal;
    private final ModelPart snout;
    private final ModelPart antennaL;
    private final ModelPart antennaR;
    private final ModelPart spines;
    private final ModelPart[] legsL = new ModelPart[3];
    private final ModelPart[] legsR = new ModelPart[3];

    public FishModel(ModelPart root) {
        this.body = root.getChild("body");
        this.tailFin = body.getChild("tail_fin");
        this.tailFan = body.getChild("tail_fan");
        this.dorsal = body.getChild("dorsal");
        this.snout = body.getChild("snout");
        this.antennaL = body.getChild("antenna_l");
        this.antennaR = body.getChild("antenna_r");
        this.spines = body.getChild("spines");
        for (int i = 0; i < 3; i++) {
            legsL[i] = body.getChild("leg_l" + i);
            legsR[i] = body.getChild("leg_r" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-2.5f, -2.5f, -6.0f, 5.0f, 5.0f, 12.0f, new CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 18.0f, 0.0f));

        // Vertical fish tail (anchovy/herring/puffer).
        body.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 18).addBox(0.0f, -2.0f, 0.0f, 0.0f, 4.0f, 5.0f),
                PartPose.offset(0.0f, 0.0f, 6.0f));

        // Flat crustacean tail fan (shrimp/lobster/crab).
        body.addOrReplaceChild("tail_fan",
                CubeListBuilder.create().texOffs(36, 10).addBox(-3.0f, 0.0f, 0.0f, 6.0f, 0.0f, 4.0f),
                PartPose.offset(0.0f, 0.0f, 6.0f));

        // Dorsal fin: texOffs(36,0) box 4x3x6.
        body.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(36, 0).addBox(-2.0f, -3.0f, -1.5f, 4.0f, 3.0f, 6.0f),
                PartPose.offset(0.0f, -2.0f, 0.5f));

        // Pointed snout: texOffs(54,16) box 2x2x3 forward of the head.
        body.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(54, 16).addBox(-1.0f, -1.0f, -3.0f, 2.0f, 2.0f, 3.0f),
                PartPose.offset(0.0f, -0.5f, -5.5f));

        // Antennae: 0x1x8 plates reaching forward past the head.
        body.addOrReplaceChild("antenna_l",
                CubeListBuilder.create().texOffs(0, 32).addBox(0.0f, -0.5f, -8.0f, 0.0f, 1.0f, 8.0f),
                PartPose.offset(0.6f, -1.2f, -6.0f));
        body.addOrReplaceChild("antenna_r",
                CubeListBuilder.create().texOffs(16, 32).addBox(0.0f, -0.5f, -8.0f, 0.0f, 1.0f, 8.0f),
                PartPose.offset(-0.6f, -1.2f, -6.0f));

        // Puffer spines: a cluster of tiny cubes (auto-advancing UVs need the
        // free 0..30 x 42..50 block of the 64x64 sheet).
        body.addOrReplaceChild("spines",
                CubeListBuilder.create().texOffs(0, 42)
                        .addBox(-3.5f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f)
                        .addBox(2.5f, -1.0f, -1.0f, 1.0f, 2.0f, 2.0f)
                        .addBox(-0.5f, -3.6f, -1.0f, 1.0f, 1.5f, 2.0f)
                        .addBox(-0.5f, 2.1f, -1.0f, 1.0f, 1.5f, 2.0f),
                PartPose.ZERO);

        // Six chitinous legs, three a side (texOffs 32,24 / 46,24 boxes 6x1x1).
        int[] zRow = {-3, 0, 3};
        for (int i = 0; i < 3; i++) {
            body.addOrReplaceChild("leg_l" + i,
                    CubeListBuilder.create().texOffs(32, 24)
                            .addBox(0.0f, -0.5f, -0.5f, 6.0f, 1.0f, 1.0f),
                    PartPose.offset(2.3f, 1.8f, zRow[i]));
            body.addOrReplaceChild("leg_r" + i,
                    CubeListBuilder.create().texOffs(46, 24)
                            .addBox(-6.0f, -0.5f, -0.5f, 6.0f, 1.0f, 1.0f),
                    PartPose.offset(-2.3f, 1.8f, zRow[i]));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Per-species build sheet: which parts exist and how the body is shaped. */
    private void applySpecies(FishType type) {
        boolean crustacean = type == FishType.CRAB || type == FishType.LOBSTER || type == FishType.SHRIMP;
        tailFin.visible = type == FishType.ANCHOVY || type == FishType.HERRING
                || type == FishType.PUFFERFISH || type == FishType.SHRIMP;
        tailFan.visible = type == FishType.LOBSTER || type == FishType.CRAB || type == FishType.SHRIMP;
        dorsal.visible = type == FishType.ANCHOVY || type == FishType.HERRING || type == FishType.PUFFERFISH;
        snout.visible = type == FishType.ANCHOVY || type == FishType.HERRING;
        antennaL.visible = type == FishType.SHRIMP || type == FishType.LOBSTER;
        antennaR.visible = antennaL.visible;
        spines.visible = type == FishType.PUFFERFISH;
        for (int i = 0; i < 3; i++) {
            legsL[i].visible = crustacean;
            legsR[i].visible = crustacean;
        }
        // Body silhouette per species.
        float sx = 1.0f, sy = 1.0f, sz = 1.0f;
        switch (type) {
            case ANCHOVY    -> { sx = 0.62f; sy = 0.66f; sz = 1.22f; }
            case HERRING    -> { sx = 0.9f;  sy = 1.0f;  sz = 1.05f; }
            case SHRIMP     -> { sx = 0.85f; sy = 0.8f;  sz = 1.1f;  }
            case CRAB       -> { sx = 1.5f;  sy = 0.52f; sz = 1.25f; }
            case PUFFERFISH -> { sx = 1.3f;  sy = 1.3f;  sz = 0.78f; }
            case LOBSTER    -> { sx = 0.95f; sy = 0.85f; sz = 1.55f; }
        }
        body.xScale = sx;
        body.yScale = sy;
        body.zScale = sz;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        FishType type = entity instanceof CustomFishEntity fish ? fish.getFishType() : FishType.HERRING;
        applySpecies(type);
        boolean crustacean = type == FishType.CRAB || type == FishType.LOBSTER || type == FishType.SHRIMP;

        // Tail beats: sleek fish flick fast, puffer paddles, crustaceans
        // use their fan for an occasional escape-jet.
        float freq = switch (type) {
            case ANCHOVY -> 1.9f;
            case HERRING -> 1.5f;
            case PUFFERFISH -> 0.9f;
            default -> 0.7f;
        };
        tailFin.yRot = Mth.cos(ageInTicks * freq) * 0.42f;
        tailFan.xRot = Mth.sin(ageInTicks * 0.55f) > 0.92f
                ? Mth.sin(ageInTicks * 3.4f) * 0.5f : 0.0f;   // craymal flip bursts

        // Whole-body swim: fish undulate, shrimp hunch, crab stays rock-steady.
        float wiggle = crustacean ? 0.03f : 0.14f + (type == FishType.SHRIMP ? 0.1f : 0.0f);
        body.yRot = Mth.cos(ageInTicks * (freq * 0.6f)) * wiggle;
        body.xRot = type == FishType.SHRIMP ? 0.3f : Mth.sin(ageInTicks * 0.3f) * 0.04f;
        body.zRot = Mth.sin(ageInTicks * 0.21f) * (crustacean ? 0.02f : 0.07f);

        // Dorsal fin ripples; antennae drift like seaweed in the current.
        dorsal.xRot = Mth.sin(ageInTicks * 1.1f) * 0.06f;
        antennaL.yRot = 0.25f + Mth.sin(ageInTicks * 0.7f) * 0.25f;
        antennaR.yRot = -0.25f + Mth.sin(ageInTicks * 0.7f + 1.4f) * 0.25f;

        // Crustacean legs row in a tripod gait while the animal moves.
        float moving = Mth.clamp(limbSwingAmount * 3.0f, 0.0f, 1.0f);
        for (int i = 0; i < 3; i++) {
            float phase = limbSwing * 1.9f + i * 2.1f;
            legsL[i].yRot = Mth.sin(phase) * 0.5f * moving + 0.1f;
            legsR[i].yRot = -Mth.sin(phase + (float) Math.PI) * 0.5f * moving - 0.1f;
            legsL[i].zRot = -Mth.abs(Mth.sin(phase)) * 0.18f * moving - 0.05f;
            legsR[i].zRot = Mth.abs(Mth.sin(phase + (float) Math.PI)) * 0.18f * moving + 0.05f;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        body.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
