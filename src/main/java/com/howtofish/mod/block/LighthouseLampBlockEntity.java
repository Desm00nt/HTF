package com.howtofish.mod.block;

import com.howtofish.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Purely a data holder driving the client-side beam renderer; the beam
 * direction slowly rotates using the entity's world time.
 */
public class LighthouseLampBlockEntity extends BlockEntity {
    public LighthouseLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHTHOUSE_LAMP.get(), pos, state);
    }
}
