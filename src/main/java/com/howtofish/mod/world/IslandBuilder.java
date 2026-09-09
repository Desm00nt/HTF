package com.howtofish.mod.world;

import com.howtofish.mod.registry.ModBlocks;
import com.howtofish.mod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Procedurally builds the "How to Fish" style lighthouse island directly with
 * block placement (no NBT structure files needed, so nothing can go missing).
 * <p>
 * The island is much richer now: an irregular coastline with a sandy beach
 * and shallow underwater shelf, a grassy meadow with palm trees, flowers and
 * rocks, a sailor's A-frame CANVAS TENT with a camp stove and Sol's stool,
 * and a grand striped lighthouse with a gallery and glass lamp room.
 * Also builds a smaller second island far away that becomes the Radar's
 * destination once the Old Man is fed a boss trophy.
 */
public class IslandBuilder {

    // Water columns of the flat ocean span Y=-12..-2 (surface plane at -1).
    // The island surface block now sits at Y=-1: the beach touches the water
    // line directly (one block lower than before), and every column below it
    // is filled solid down to the natural stone floor at Y=-16, so there is
    // no dangling water pocket / void under the island any more.
    public static final BlockPos SPAWN_ISLAND_ORIGIN = new BlockPos(0, -1, 0);
    public static final BlockPos SECOND_ISLAND_ORIGIN = new BlockPos(1536, -1, -1216);

    /** Sol's stool and the moored boat: THE places things must be at - the
        /howtofish fix command uses these anchors to put anything that drifted
        (or got flung) back exactly where the island expects it. */
    public static final BlockPos OLD_MAN_HOME = SPAWN_ISLAND_ORIGIN.offset(-2, 1, 4);
    public static final BlockPos BOAT_HOME = SPAWN_ISLAND_ORIGIN.offset(-8, -1, 23);

    private static final int LIGHTHOUSE_TOP_Y = 17; // Y offset of the lamp inside the lamp room

    public static BlockPos getLighthouseLampPos() {
        return SPAWN_ISLAND_ORIGIN.offset(8, 18, -8);
    }

    public static void buildSpawnIsland(ServerLevel level) {
        BlockPos origin = SPAWN_ISLAND_ORIGIN;
        buildSandIsland(level, origin, 14, 1337);
        buildLighthouse(level, origin.offset(8, 0, -8));
        buildTent(level, origin.offset(-4, 0, 0));
        buildPier(level, origin.offset(-6, 0, 8));
        decorateMeadow(level, origin, 1337);
        // Past the pier head in open water - spawning beside the
        // planks just dropped the boat under the pier deck.
        spawnBoat(level, BOAT_HOME);
        // Sol sits on his stool by the tent door and never leaves it.
        spawnOldMan(level, OLD_MAN_HOME);
    }

    public static void buildSecondIsland(ServerLevel level) {
        buildSandIsland(level, SECOND_ISLAND_ORIGIN, 11, 9001);
        // A small camp to mark "island 2": campfire, crates and palms.
        BlockPos origin = SECOND_ISLAND_ORIGIN;
        level.setBlock(origin.offset(2, 1, 2), Blocks.CAMPFIRE.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, 1, 1), Blocks.BARREL.defaultBlockState(), 3);
        level.setBlock(origin.offset(1, 1, 3), Blocks.BARREL.defaultBlockState(), 3);
        level.setBlock(origin.offset(3, 1, 3), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        buildPalm(level, origin.offset(-5, 1, -4), 5);
        buildPalm(level, origin.offset(4, 1, -6), 4);
        buildRock(level, origin.offset(-6, 1, 5), 2);
    }

    /** Irregular sand island with beach ring and a shallow underwater shelf. */
    private static void buildSandIsland(ServerLevel level, BlockPos center, int radius, long seed) {
        RandomSource random = RandomSource.create(seed);
        double wobbleA = 1.2 + random.nextDouble() * 0.9;
        double wobbleB = 0.8 + random.nextDouble() * 0.7;
        double phaseA = random.nextDouble() * Math.PI * 2;
        double phaseB = random.nextDouble() * Math.PI * 2;

        int r = radius + 5; // shelf margin
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt(x * x + z * z);
                double theta = Math.atan2(z, x);
                double coast = radius
                        + Math.sin(theta * 3 + phaseA) * wobbleA
                        + Math.sin(theta * 5 + phaseB) * wobbleB;
                if (dist > coast + 5) continue;

                BlockPos top = center.offset(x, 0, z);

                if (dist <= coast) {
                    // Island body: beach ring then grass.
                    BlockState surface = dist > coast - 2.5 ? Blocks.SAND.defaultBlockState()
                            : Blocks.GRASS_BLOCK.defaultBlockState();
                    level.setBlock(top, surface, 3);
                    fillToSeafloor(level, top);
                    // Walkable air above.
                    for (int y = 1; y <= 7; y++) {
                        level.setBlock(top.above(y), Blocks.AIR.defaultBlockState(), 3);
                    }
                } else {
                    // Shallow sandy shelf fading into the sea. Same solid
                    // foundation all the way down - the old 4-block stub left
                    // the sea hanging under the island edge.
                    fillToSeafloor(level, top);
                }
            }
        }
    }

    /**
     * Solid foundation for any island column: sand, sandstone, then stone all
     * the way to the flat world's natural stone floor (Y=-16 for the spawn
     * island's depth) - nothing can show through as a floating water pocket.
     */
    private static void fillToSeafloor(ServerLevel level, BlockPos top) {
        for (int dy = 1; dy <= 15; dy++) {
            BlockState under;
            if (dy <= 2) under = Blocks.SAND.defaultBlockState();
            else if (dy == 3) under = Blocks.SANDSTONE.defaultBlockState();
            else under = Blocks.STONE.defaultBlockState();
            level.setBlock(top.below(dy), under, 3);
        }
    }

    /** Meadow decoration: palms, flowers, tall grass and rocks. */
    private static void decorateMeadow(ServerLevel level, BlockPos origin, long seed) {
        RandomSource random = RandomSource.create(seed);
        // Palms around the meadow.
        buildPalm(level, origin.offset(-9, 1, -5), 6);
        buildPalm(level, origin.offset(-8, 1, 4), 5);
        buildPalm(level, origin.offset(3, 1, 9), 6);
        buildPalm(level, origin.offset(10, 1, 4), 5);

        // Rocks.
        buildRock(level, origin.offset(5, 1, -6), 2);
        buildRock(level, origin.offset(-9, 1, 9), 1);
        buildRock(level, origin.offset(10, 1, -3), 1);

        // Flowers & tall grass scattered on grass blocks.
        for (int i = 0; i < 90; i++) {
            int x = random.nextInt(27) - 13;
            int z = random.nextInt(27) - 13;
            BlockPos pos = origin.offset(x, 1, z);
            if (!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) continue;
            if (!level.getBlockState(pos).isAir()) continue;
            BlockState plant = switch (random.nextInt(10)) {
                case 0 -> Blocks.POPPY.defaultBlockState();
                case 1 -> Blocks.DANDELION.defaultBlockState();
                case 2 -> Blocks.CORNFLOWER.defaultBlockState();
                case 3 -> Blocks.AZURE_BLUET.defaultBlockState();
                case 4 -> Blocks.OXEYE_DAISY.defaultBlockState();
                case 5, 6 -> Blocks.TALL_GRASS.defaultBlockState();
                default -> Blocks.GRASS.defaultBlockState();
            };
            level.setBlock(pos, plant, 3);
        }
    }

    /** A stylised palm tree: tall log, leaning, with a burst of leaves. */
    private static void buildPalm(ServerLevel level, BlockPos base, int height) {
        RandomSource random = RandomSource.create(base.hashCode());
        int lean = random.nextInt(2) * 2 - 1; // -1 or 1
        BlockPos top = base;
        for (int i = 0; i < height; i++) {
            level.setBlock(top, Blocks.OAK_LOG.defaultBlockState(), 3);
            top = top.above();
            if (i == height - 3) top = top.offset(lean, 0, 0);
            if (i == height - 2) top = top.offset(lean, 0, 0);
        }
        // Fronds.
        BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState();
        level.setBlock(top, leaf, 3);
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {lean, lean}}) {
            level.setBlock(top.offset(d[0], 0, d[1]), leaf, 3);
            level.setBlock(top.offset(d[0] * 2, -1, d[1] * 2), leaf, 3);
        }
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            level.setBlock(top.offset(d[0], -1, d[1]), leaf, 3);
        }
    }

    private static void buildRock(ServerLevel level, BlockPos base, int size) {
        BlockState stone = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        BlockState plain = Blocks.COBBLESTONE.defaultBlockState();
        level.setBlock(base, stone, 3);
        if (size >= 1) {
            level.setBlock(base.east(), plain, 3);
            level.setBlock(base.north(), stone, 3);
        }
        if (size >= 2) {
            level.setBlock(base.east().north(), plain, 3);
            level.setBlock(base.above(), plain, 3);
            level.setBlock(base.west(), stone, 3);
        }
    }

    /**
     * A proper lighthouse: tapered tower with red-and-white stripes, side
     * windows, a gallery with railings, a glass lamp room with the lamp block
     * and a small dome with a lightning rod.
     */
    private static void buildLighthouse(ServerLevel level, BlockPos base) {
        BlockState white = ModBlocks.LIGHTHOUSE_BRICKS.get().defaultBlockState();
        BlockState red = Blocks.RED_CONCRETE.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();

        // ---- Foundation plinth: a wide brick skirt around the base. ----
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 2.0 && dist <= 3.2) {
                    level.setBlock(base.offset(x, 1, z), dist > 2.8 ? bricks : white, 3);
                }
            }
        }

        // ---- Striped tower: classic alternating red / white bands. ----
        int tallH = 16;
        for (int y = 0; y < tallH; y++) {
            float ring = y < 9 ? 1.9f : 1.4f;
            // Two-row red bands on white, classic lighthouse look.
            BlockState wall = (y / 2) % 2 == 0 ? red : white;
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist <= ring + 0.3) {
                        BlockPos pos = base.offset(x, y + 1, z);
                        boolean edge = dist > ring - 0.6;
                        level.setBlock(pos, edge ? wall : Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            // Windows with a white sill on all four sides.
            if (y == 4 || y == 8 || y == 12) {
                level.setBlock(base.offset(0, y + 1, 2), glass, 3);
                level.setBlock(base.offset(2, y + 1, 0), glass, 3);
                level.setBlock(base.offset(0, y + 1, -2), glass, 3);
                level.setBlock(base.offset(-2, y + 1, 0), glass, 3);
            }
            if (y == 3 || y == 7 || y == 11) {
                level.setBlock(base.offset(0, y + 1, 2), white, 3);
                level.setBlock(base.offset(2, y + 1, 0), white, 3);
                level.setBlock(base.offset(0, y + 1, -2), white, 3);
                level.setBlock(base.offset(-2, y + 1, 0), white, 3);
            }
        }

        // ---- Gallery deck: slab ring + FULL oak fence railing. ----
        int galleryY = tallH + 1;
        BlockState deck = Blocks.STONE_BRICK_SLAB.defaultBlockState();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 1.6 && dist <= 2.3) {
                    level.setBlock(base.offset(x, galleryY, z), deck, 3);
                } else if (dist <= 1.6) {
                    level.setBlock(base.offset(x, galleryY, z), white, 3);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > 1.9 && dist <= 2.3) {
                    level.setBlock(base.offset(x, galleryY + 1, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                }
            }
        }

        // ---- Lamp room: corner posts + glass on two levels, lamp glowing inside. ----
        int lampY = galleryY + 1;
        for (int dy = 0; dy < 2; dy++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) == 1 && Math.abs(z) == 1) {
                        level.setBlock(base.offset(x, lampY + dy, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                    } else if (Math.abs(x) == 1 || Math.abs(z) == 1) {
                        level.setBlock(base.offset(x, lampY + dy, z), glass, 3);
                    }
                }
            }
        }
        // The lamp sits on the gallery centre column.
        level.setBlock(base.offset(0, lampY, 0), ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState(), 3);
        level.setBlock(base.offset(0, lampY + 1, 0), Blocks.AIR.defaultBlockState(), 3);

        // ---- Dome roof: dark oak cap + stone finial + lightning rod. ----
        int roofY = lampY + 2;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(base.offset(x, roofY, z), Blocks.DARK_OAK_SLAB.defaultBlockState(), 3);
            }
        }
        level.setBlock(base.offset(0, roofY + 1, 0), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
        level.setBlock(base.offset(0, roofY + 2, 0), Blocks.LIGHTNING_ROD.defaultBlockState(), 3);

        // ---- Entrance: arched doorway, step, flanking lanterns. ----
        level.setBlock(base.offset(0, 1, 2), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 2, 2), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 3, 2), white, 3); // arch top
        level.setBlock(base.offset(0, 1, 3), Blocks.STONE_BRICK_STAIRS.defaultBlockState(), 3);
        level.setBlock(base.offset(1, 1, 3), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(base.offset(-1, 1, 3), Blocks.LANTERN.defaultBlockState(), 3);
    }

    /**
     * A sailor's camp instead of the old wooden shack: a stepped A-frame
     * canvas tent (light grey concrete reads as stretched canvas), a dark
     * trim, trapdoor door flaps, Sol's stool in front of the door, a camp
     * fire ring and an open supply shed (fence posts + plank roof) offside.
     */
    private static void buildTent(ServerLevel level, BlockPos base) {
        BlockState canvas = Blocks.WHITE_WOOL.defaultBlockState();
        BlockState trim = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState post = Blocks.OAK_FENCE.defaultBlockState();

        // A-frame: rows 1..4, footprint shrinking by one per side per row.
        int half = 4; // half-width along X at row 1
        int depth = 5; // Z extent
        for (int row = 1; row <= 4; row++) {
            int inset = row - 1;
            int x0 = -half + inset;
            int x1 = half - inset;
            for (int z = -depth / 2; z <= depth / 2; z++) {
                // Solid stepped walls; hollow the middle so the door shows depth.
                for (int x = x0; x <= x1; x++) {
                    boolean edge = x == x0 || x == x1 || z == -depth / 2 || z == depth / 2;
                    boolean ridge = row == 4;
                    if (edge || ridge) {
                        level.setBlock(base.offset(x, row, z), canvas, 3);
                    }
                }
            }
        }
        // Ridgepole peeking out both ends + a hanging lantern.
        int top = 5;
        level.setBlock(base.offset(0, top, -depth / 2 - 1), post, 3);
        level.setBlock(base.offset(0, top, depth / 2 + 1), post, 3);
        level.setBlock(base.offset(0, top + 1, depth / 2 + 1), Blocks.LANTERN.defaultBlockState(), 3);
        // Dark trim band along the bottom + gold-ish pennant block on the ridge.
        for (int z = -depth / 2; z <= depth / 2; z++) {
            level.setBlock(base.offset(-half, 1, z), trim, 3);
            level.setBlock(base.offset(half, 1, z), trim, 3);
        }
        // Doorway facing +Z (toward the pier): carve 1 wide x 2 tall + flaps.
        for (int row = 1; row <= 2; row++) {
            level.setBlock(base.offset(0, row, depth / 2), Blocks.AIR.defaultBlockState(), 3);
        }
        level.setBlock(base.offset(-1, 1, depth / 2), Blocks.SPRUCE_TRAPDOOR.defaultBlockState(), 3);
        level.setBlock(base.offset(1, 1, depth / 2), Blocks.SPRUCE_TRAPDOOR.defaultBlockState(), 3);
        // Tent floor & a bedroll inside.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(base.offset(x, 0, z), plank, 3);
            }
        }
        // Rolled-up bedroll + a pillow (a real bed needs its foot half, and
        // we don't want him napping during work).
        level.setBlock(base.offset(-2, 1, -1), Blocks.BROWN_WOOL.defaultBlockState(), 3);
        level.setBlock(base.offset(-2, 1, 0), Blocks.QUARTZ_SLAB.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 1, 0), Blocks.CYAN_CARPET.defaultBlockState(), 3);

        // Sol's stool right at the door (he sits here, immortal, forever).
        level.setBlock(base.offset(2, 0, depth / 2 + 2), Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);

        // Camp fire ring off to the side + supply shed.
        BlockPos fire = base.offset(-6, 0, 4);
        level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState(), 3);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                level.setBlock(fire.offset(dx, -1, dz), Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
            }
        }
        BlockPos shed = base.offset(7, 0, 3);
        for (int dx = 0; dx <= 2; dx++) {
            for (int dz = 0; dz <= 2; dz++) {
                level.setBlock(shed.offset(dx, 0, dz), plank, 3);
            }
        }
        level.setBlock(shed.offset(0, 1, 0), post, 3);
        level.setBlock(shed.offset(2, 1, 0), post, 3);
        level.setBlock(shed.offset(0, 1, 2), post, 3);
        level.setBlock(shed.offset(2, 1, 2), post, 3);
        for (int dx = -1; dx <= 3; dx++) {
            for (int dz = -1; dz <= 3; dz++) {
                level.setBlock(shed.offset(dx, 2, dz), Blocks.OAK_SLAB.defaultBlockState(), 3);
            }
        }
        level.setBlock(shed.offset(1, 1, 1), Blocks.BARREL.defaultBlockState(), 3);
        level.setBlock(shed.offset(2, 1, 1), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(shed.offset(0, 1, 1), Blocks.SMOKER.defaultBlockState(), 3);
    }

    /** A proper pier: 3-wide planks on fence posts with rope-style railings. */
    private static void buildPier(ServerLevel level, BlockPos start) {
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState post = Blocks.OAK_FENCE.defaultBlockState();
        BlockState rail = Blocks.OAK_FENCE.defaultBlockState();

        for (int i = 0; i < 11; i++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos p = start.offset(side, 0, i);
                level.setBlock(p, plank, 3);
                level.setBlock(p.below(1), post, 3);   // support posts into the water
                level.setBlock(p.below(2), post, 3);
            }
            // Railings along both sides with gaps.
            if (i % 2 == 1) {
                level.setBlock(start.offset(-1, 0, i), rail, 3);
                level.setBlock(start.offset(1, 0, i), rail, 3);
            }
        }
        // Little crates at the pier head.
        level.setBlock(start.offset(0, 0, -1), Blocks.BARREL.defaultBlockState(), 3);
        level.setBlock(start.offset(0, -2, 3), Blocks.OAK_FENCE.defaultBlockState(), 3);
    }

    private static void spawnBoat(ServerLevel level, BlockPos pos) {
        Boat boat = new Boat(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        boat.setYRot(90.0f);
        level.addFreshEntity(boat);
    }

    private static void spawnOldMan(ServerLevel level, BlockPos pos) {
        var oldMan = ModEntities.OLD_MAN.get().create(level);
        if (oldMan != null) {
            oldMan.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            oldMan.setHomePos(pos);   // the fix-command anchor travels with him
            oldMan.setCustomName(net.minecraft.network.chat.Component.translatable("entity.howtofish.old_man"));
            oldMan.setCustomNameVisible(true);
            level.addFreshEntity(oldMan);
        }
    }
}
