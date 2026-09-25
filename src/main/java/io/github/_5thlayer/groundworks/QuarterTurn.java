// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.groundworks;

import com.mojang.serialization.Codec;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * How far the held stack's next placement is turned from the way the player looks, a quarter
 * clockwise per step, 0 to 3. The turn is relative to the look rather than a compass direction,
 * because the player's camera turns: the same press means the same turn wherever they stand.
 * {@link #NONE} is also what a stack with no turn reads as.
 */
public record QuarterTurn(int quarters) {

    public static final QuarterTurn NONE = new QuarterTurn(0);

    public QuarterTurn {
        if (quarters < 0 || quarters > 3) {
            throw new IllegalArgumentException("a quarter turn is 0 to 3, not " + quarters);
        }
    }

    public static QuarterTurn of(int quarters) {
        return new QuarterTurn(Math.floorMod(quarters, 4));
    }

    public QuarterTurn rotate() {
        return of(quarters + 1);
    }

    public QuarterTurn reverseRotate() {
        return of(quarters - 1);
    }

    /** The direction turned clockwise, seen from above; up and down are no heading and stay. */
    public Direction turn(Direction direction) {
        if (direction.getAxis().isVertical()) {
            return direction;
        }
        // The 2D data values run clockwise: south, west, north, east.
        return Direction.from2DDataValue(direction.get2DDataValue() + quarters);
    }

    public float turnYaw(float yaw) {
        return yaw + 90f * quarters;
    }

    // Built when the component registers, as DismantleStart's is.
    static DataComponentType<QuarterTurn> componentType() {
        return DataComponentType.<QuarterTurn>builder()
                .persistent(Codec.intRange(0, 3).xmap(QuarterTurn::new, QuarterTurn::quarters))
                .networkSynchronized(ByteBufCodecs.VAR_INT.map(QuarterTurn::new, QuarterTurn::quarters))
                .build();
    }
}
