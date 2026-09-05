package com.howtofish.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The bright lighthouse lamp block - always lit, and renders a slowly rotating
 * light beam upward via {@link com.howtofish.mod.block.LighthouseLampBlockEntity}
 * (reusing the vanilla beacon beam shader for the "cool lighthouse" look).
 */
public class LighthouseLampBlock extends Block implements EntityBlock {

    public LighthouseLampBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LighthouseLampBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
