// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks.mixin;

import io.github._5thlayer.groundworks.Raise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where a placement goes, moved straight up or down by the held stack's
 * {@linkplain Raise#heightOf(BlockPlaceContext) height}, and the click's location with it, so a
 * block that reads where on the face the click landed -- stairs, a slab, a trapdoor -- reads the
 * same half moved as unmoved. Which column it goes in is vanilla's: the clicked block if it is
 * replaceable, the spot across the clicked face otherwise.
 *
 * <p>A moved spot is placed into only if it is inside the world and replaceable itself, since it
 * is never the block that was clicked. A context made by {@link BlockPlaceContext#at}, as an item's
 * {@code updatePlacementContext} makes one, names a spot worked out from a context already moved,
 * so it is not moved again.
 */
@Mixin(BlockPlaceContext.class)
public abstract class BlockPlaceContextHeightMixin extends UseOnContext {

    @Unique
    private boolean groundworks$namesItsSpot;

    private BlockPlaceContextHeightMixin(Level level, @Nullable Player player, InteractionHand hand, ItemStack stack,
                                         BlockHitResult hit) {
        super(level, player, hand, stack, hit);
    }

    @Unique
    private int groundworks$height() {
        return groundworks$namesItsSpot ? 0 : Raise.heightOf((BlockPlaceContext) (Object) this).blocks();
    }

    @Inject(method = "at", at = @At("RETURN"))
    private static void groundworks$notMovedAgain(BlockPlaceContext context, BlockPos pos, Direction direction,
                                                  CallbackInfoReturnable<BlockPlaceContext> cir) {
        ((BlockPlaceContextHeightMixin) (Object) cir.getReturnValue()).groundworks$namesItsSpot = true;
    }

    @Inject(method = "getClickedPos", at = @At("RETURN"), cancellable = true)
    private void groundworks$movePos(CallbackInfoReturnable<BlockPos> cir) {
        int height = groundworks$height();
        if (height != 0) {
            cir.setReturnValue(cir.getReturnValue().above(height));
        }
    }

    @Override
    public Vec3 getClickLocation() {
        return super.getClickLocation().add(0, groundworks$height(), 0);
    }

    @Inject(method = "canPlace", at = @At("HEAD"), cancellable = true)
    private void groundworks$placeIntoMovedSpot(CallbackInfoReturnable<Boolean> cir) {
        if (groundworks$height() != 0) {
            BlockPos moved = getClickedPos();
            cir.setReturnValue(!getLevel().isOutsideBuildHeight(moved)
                    && getLevel().getBlockState(moved).canBeReplaced((BlockPlaceContext) (Object) this));
        }
    }
}
