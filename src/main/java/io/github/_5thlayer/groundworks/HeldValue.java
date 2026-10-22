package io.github._5thlayer.groundworks;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A value the held stack carries as one data component, such as its turn or its stored Stretch. A
 * stack without the component reads as {@code none}, and storing {@code none} removes the
 * component, so a stack put back to it stacks again with one never touched. The type is looked up
 * only when the value is, so naming one loads no registry, as a plain JVM's tests need.
 */
record HeldValue<T>(Supplier<DataComponentType<T>> type, @Nullable T none) {

    /** The stack's value, or {@code none} when it carries none. */
    @Nullable T get(ItemStack stack) {
        T value = stack.get(type.get());
        return value != null ? value : none;
    }

    /** Stores the value on the stack, removing the component when it is {@code none}. */
    void set(ItemStack stack, @Nullable T value) {
        if (value == null || Objects.equals(value, none)) {
            stack.remove(type.get());
        } else {
            stack.set(type.get(), value);
        }
    }
}
