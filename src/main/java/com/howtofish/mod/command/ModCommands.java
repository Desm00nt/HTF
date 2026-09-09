package com.howtofish.mod.command;

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
 * {@code /howtofish fix} - a gentle state doctor for the island. Nothing is
 * deleted or rebuilt: it only verifies that the FIXED things are at their
 * places and puts them back when they are not - Old Sol (e.g. flung off his
 * stool by a stray explosion or physics hiccup) and the moored boat (pushed
 * away by waves, or despawned). Free-running entities (fish, the boss) are
 * deliberately left alone.
 */
public final class ModCommands {

    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("howtofish")
                .then(Commands.literal("fix")
                        .executes(ctx -> fix(ctx.getSource()))));
    }

    private static int fix(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        int solFixed = 0;
        int boatFixed = 0;

        // --- Sol back onto his stool (anchor = the stool he was born on) ---
        AABB everything = new AABB(-4.0E7, 0.0, -4.0E7, 4.0E7, 320.0, 4.0E7);
        for (OldManEntity sol : level.getEntitiesOfClass(OldManEntity.class, everything)) {
            BlockPos home = sol.getHomePos();
            if (needsPutingBack(sol, home, 1.2)) {
                sol.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                sol.setDeltaMovement(Vec3.ZERO);
                sol.setYRot(0.0F);             // stool faces the sea (south)
                sol.setXRot(0.0F);
                sol.yHeadRot = 0.0F;
                sol.setHealth(sol.getMaxHealth());
                solFixed++;
            }
        }

        // --- the boat back to its mooring post, or a new one if it drowned --
        BlockPos spot = IslandBuilder.BOAT_HOME;
        Boat boat = level.getEntitiesOfClass(Boat.class,
                        new AABB(spot).inflate(48.0))
                .stream().filter(Entity::isAlive).findFirst().orElse(null);
        if (boat == null) {
            Boat fresh = new Boat(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            fresh.setYRot(90.0f);
            level.addFreshEntity(fresh);
            boatFixed++;
        } else if (needsPutingBack(boat, spot, 3.5)) {
            boat.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            boat.setDeltaMovement(Vec3.ZERO);
            boat.setYRot(90.0f);
            boatFixed++;
        }

        final int fSol = solFixed, fBoat = boatFixed;
        // NOTE: this mapping set's sendSuccess takes a plain Component
        // (the Supplier variant is a later-version thing).
        Component msg = fSol == 0 && fBoat == 0
                ? Component.translatable("command.howtofish.fix.clean")
                : Component.translatable("command.howtofish.fix.result", fSol, fBoat);
        src.sendSuccess(msg, true);
        return 1 + fSol + fBoat;
    }

    /** Horizontal drift beyond `slack` blocks means "not in place". */
    private static boolean needsPutingBack(Entity e, BlockPos home, double slack) {
        double dx = e.getX() - (home.getX() + 0.5);
        double dz = e.getZ() - (home.getZ() + 0.5);
        return dx * dx + dz * dz > slack * slack;
    }
}
