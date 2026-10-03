// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.compat;

import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.groundworks.FootprintPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade reads a {@linkplain FootprintPartBlock Footprint's part} as its origin, header and icon
 * included, so every block of a footprint shows its origin's line (ADR 0009). A part has no block
 * entity and no item, so without this Jade names the part with a blank icon.
 *
 * <p>Jade finds this by its annotation, so it is loaded only where Jade is installed.
 */
@WailaPlugin
public class FootprintJadePlugin implements IWailaPlugin {

    private static Accessor<?> originFor(IWailaClientRegistration registration, Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block) || !(block.getBlock() instanceof FootprintPartBlock part)) {
            return accessor;
        }
        BlockPos origin = part.footprint().standingOrigin(block.getLevel(), block.getPosition(), block.getBlockState());
        if (origin == null) {
            return accessor;
        }
        BlockState originState = block.getLevel().getBlockState(origin);
        return registration.blockAccessor()
                .from(block)
                .hit(block.getHitResult().withPosition(origin))
                .blockState(originState)
                .blockEntity(block.getLevel().getBlockEntity(origin))
                .build();
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addRayTraceCallback((hit, accessor, original) -> originFor(registration, accessor));
    }
}
