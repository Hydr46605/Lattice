package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

@InternalApi
public final class ReflectiveItemsAdderItemService implements ItemsAdderItemService {
    private final Class<?> customStack;
    private final Method isInRegistry;
    private final Method getInstance;
    private final Method byItemStack;

    public ReflectiveItemsAdderItemService(Class<?> customStack, Method isInRegistry, Method getInstance, Method byItemStack) {
        this.customStack = Preconditions.requireNonNull(customStack, "customStack");
        this.isInRegistry = Preconditions.requireNonNull(isInRegistry, "isInRegistry");
        this.getInstance = Preconditions.requireNonNull(getInstance, "getInstance");
        this.byItemStack = Preconditions.requireNonNull(byItemStack, "byItemStack");
        this.isInRegistry.setAccessible(true);
        this.getInstance.setAccessible(true);
        this.byItemStack.setAccessible(true);
    }

    @Override
    public boolean exists(String itemId) {
        Preconditions.requireText(itemId, "itemId");
        try {
            return Boolean.TRUE.equals(isInRegistry.invoke(null, itemId));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return false;
        }
    }

    @Override
    public Optional<ItemStack> item(String itemId) {
        Preconditions.requireText(itemId, "itemId");
        try {
            Object stack = getInstance.invoke(null, itemId);
            if (stack == null) {
                return Optional.empty();
            }
            Method getItemStack = stack.getClass().getMethod("getItemStack");
            getItemStack.setAccessible(true);
            Object itemStack = getItemStack.invoke(stack);
            return itemStack instanceof ItemStack bukkitItem ? Optional.of(bukkitItem) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> itemId(ItemStack itemStack) {
        Preconditions.requireNonNull(itemStack, "itemStack");
        try {
            Object stack = byItemStack.invoke(null, itemStack);
            if (stack == null) {
                return Optional.empty();
            }
            Method getNamespacedId = stack.getClass().getMethod("getNamespacedID");
            getNamespacedId.setAccessible(true);
            Object id = getNamespacedId.invoke(stack);
            return id instanceof String itemId && !itemId.isBlank() ? Optional.of(itemId) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    public Class<?> customStackClass() {
        return customStack;
    }
}
