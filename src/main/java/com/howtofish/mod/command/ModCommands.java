package com.howtofish.mod.command;

import com.howtofish.mod.HowToFishMod;
import com.howtofish.mod.entity.OldManEntity;
import com.howtofish.mod.world.IslandBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /howtofish fix} (alias {@code /howtofish чинить}) - a gentle state
 * doctor for the island. NOTHING is deleted or rebuilt: it only verifies that
 * the fixed things are at their places and puts them back when they are not.
 * Old Sol is teleported onto his stool (his home is persisted in NBT) - and
 * if he somehow got despawned entirely, a fresh one is put back at the stool.
 * The moored boat is dragged back to its post, or a new one spawns if it
 * drowned. Free-running entities (fish, the boss, items) are untouched.
 */
public final class ModCommands {

    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("howtofish")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("fix").executes(ctx -> fix(ctx.getSource())))
                // RU alias, so the command is findable without English too
                .then(Commands.literal("чинить").executes(ctx -> fix(ctx.getSource())));
        dispatcher.register(root);
        HowToFishMod.LOGGER.info("[HowToFish] registered command /howtofish fix");
    }

    private static int help(CommandSourceStack src) {
        src.sendSuccess(Component.translatable("command.howtofish.help"), false);
        return 1;
    }

    private static int fix(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos home = IslandBuilder.OLD_MAN_HOME;
        int solFixed = 0;
        int boatFixed = 0;

        // --- Sol: everyone back onto their stool; zero Sol -> spawn THE Sol -
        var sols = level.getEntitiesOfClass(OldManEntity.class,
                new AABB(home).inflate(128.0, 200.0, 128.0));
        if (sols.isEmpty()) {
            IslandBuilder.spawnOldMan(level, home);
            solFixed++;
        } else {
            for (OldManEntity sol : sols) {
                BlockPos anchor = sol.getHomePos();
                if (needsPutingBack(sol, anchor, 1.2)) {
                    sol.teleportTo(anchor.getX() + 0.5, anchor.getY(), anchor.getZ() + 0.5);
                    sol.setDeltaMovement(Vec3.ZERO);
                    sol.setYRot(0.0F);             // stool faces the sea (south)
                    sol.setXRot(0.0F);
                    sol.yHeadRot = 0.0F;
                    sol.setHealth(sol.getMaxHealth());
                    sol.fallDistance = 0.0f;
                    solFixed++;
                }
            }
        }

        // --- the boat back to its mooring post, or a new one if it drowned --
        BlockPos spot = IslandBuilder.BOAT_HOME;
        Boat boat = level.getEntitiesOfClass(Boat.class,
                        new AABB(spot).inflate(48.0))
                .stream().filter(Entity::isAlive).findFirst().orElse(null);
        if (boat == null) {
            IslandBuilder.spawnBoat(level, spot);
            boatFixed++;
        } else if (needsPutingBack(boat, spot, 3.5)) {
            boat.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            boat.setDeltaMovement(Vec3.ZERO);
            boat.setYRot(90.0f);
            boatFixed++;
        }

        HowToFishMod.LOGGER.info("[HowToFish] fix: sol={} boat={}", solFixed, boatFixed);
        Component msg = solFixed == 0 && boatFixed == 0
                ? Component.translatable("command.howtofish.fix.clean")
                : Component.translatable("command.howtofish.fix.result", solFixed, boatFixed);
        src.sendSuccess(msg, true);
        return 1 + solFixed + boatFixed;
    }

    /** Horizontal drift beyond `slack` blocks means "not in place". */
    private static boolean needsPutingBack(Entity e, BlockPos home, double slack) {
        double dx = e.getX() - (home.getX() + 0.5);
        double dz = e.getZ() - (home.getZ() + 0.5);
        return dx * dx + dz * dz > slack * slack;
    }
}
