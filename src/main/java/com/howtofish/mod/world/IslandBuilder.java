package com.howtofish.mod.world;

import com.howtofish.mod.registry.ModBlocks;
import com.howtofish.mod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Procedurally builds the "How to Fish" style lighthouse island directly with
 * block placement (no NBT structure files needed, so nothing can go missing).
 * Also builds a smaller second island far away that becomes the Radar's
 * destination once the Old Man is fed a boss trophy.
 */
public class IslandBuilder {

    public static final BlockPos SPAWN_ISLAND_ORIGIN = new BlockPos(0, 63, 0);
    public static final BlockPos SECOND_ISLAND_ORIGIN = new BlockPos(1536, 63, -1216);

    public static void buildSpawnIsland(ServerLevel level) {
        BlockPos origin = SPAWN_ISLAND_ORIGIN;
        buildSandIsland(level, origin, 13);
        buildLighthouse(level, origin.offset(8, 0, -8));
        buildDock(level, origin.offset(-6, 0, 9));
        spawnBoat(level, origin.offset(-9, 1, 12));
        spawnOldMan(level, origin.offset(2, 1, 2));
    }

    public static void buildSecondIsland(ServerLevel level) {
        buildSandIsland(level, SECOND_ISLAND_ORIGIN, 10);
        // A simple palm-less rocky outcrop to mark "island 2" until the mod is expanded further.
        for (int i = 0; i < 5; i++) {
            level.setBlock(SECOND_ISLAND_ORIGIN.above(1 + i).offset(2, 0, 2), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
        }
    }

    private static void buildSandIsland(ServerLevel level, BlockPos center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > radius) continue;
                BlockPos top = center.offset(x, 0, z);
                BlockState surface = dist > radius - 2 ? Blocks.SAND.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                level.setBlock(top, surface, 3);
                level.setBlock(top.below(1), Blocks.SANDSTONE.defaultBlockState(), 3);
                for (int y = 2; y <= 6; y++) {
                    level.setBlock(top.below(y), Blocks.STONE.defaultBlockState(), 3);
                }
                level.setBlock(top.below(7), Blocks.BEDROCK.defaultBlockState(), 3);
                // clear a couple of blocks of air above for walkable space
                level.setBlock(top.above(1), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(top.above(2), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void buildLighthouse(ServerLevel level, BlockPos base) {
        int height = 16;
        int radius = 3;
        BlockState wall = ModBlocks.LIGHTHOUSE_BRICKS.get().defaultBlockState();
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist > radius - 0.5 && dist < radius + 0.5) {
                        level.setBlock(base.offset(x, y + 1, z), wall, 3);
                    } else if (dist <= radius - 0.5) {
                        level.setBlock(base.offset(x, y + 1, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        // lamp room ring + light
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= radius + 0.5) {
                    level.setBlock(base.offset(x, height + 1, z), Blocks.GLASS.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(base.above(height + 2), ModBlocks.LIGHTHOUSE_LAMP.get().defaultBlockState(), 3);
        level.setBlock(base.above(height + 3), Blocks.LIGHTNING_ROD.defaultBlockState(), 3);
        // simple door opening
        level.setBlock(base.offset(0, 1, radius), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(base.offset(0, 2, radius), Blocks.AIR.defaultBlockState(), 3);
    }

    private static void buildDock(ServerLevel level, BlockPos start) {
        for (int i = 0; i < 10; i++) {
            BlockPos p = start.offset(0, -1, i);
            level.setBlock(p, Blocks.OAK_PLANKS.defaultBlockState(), 3);
            if (i % 3 == 0) {
                level.setBlock(p.below(2), Blocks.OAK_FENCE.defaultBlockState(), 3);
            }
        }
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
            oldMan.setCustomName(net.minecraft.network.chat.Component.translatable("entity.howtofish.old_man"));
            oldMan.setCustomNameVisible(true);
            level.addFreshEntity(oldMan);
        }
    }
}
