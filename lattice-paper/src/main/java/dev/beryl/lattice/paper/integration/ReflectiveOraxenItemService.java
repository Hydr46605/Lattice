package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

@InternalApi
public final class ReflectiveOraxenItemService implements OraxenItemService {
    private final Class<?> oraxenItems;
    private final Method isAnItem;
    private final Method getItemById;
    private final Method getIdByItem;

    public ReflectiveOraxenItemService(Class<?> oraxenItems, Method isAnItem, Method getItemById, Method getIdByItem) {
        this.oraxenItems = Preconditions.requireNonNull(oraxenItems, "oraxenItems");
        this.isAnItem = Preconditions.requireNonNull(isAnItem, "isAnItem");
        this.getItemById = Preconditions.requireNonNull(getItemById, "getItemById");
        this.getIdByItem = Preconditions.requireNonNull(getIdByItem, "getIdByItem");
        this.isAnItem.setAccessible(true);
        this.getItemById.setAccessible(true);
        this.getIdByItem.setAccessible(true);
    }

    @Override
    public boolean exists(String itemId) {
        Preconditions.requireText(itemId, "itemId");
        try {
            return Boolean.TRUE.equals(isAnItem.invoke(null, itemId));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return false;
        }
    }

    @Override
    public Optional<ItemStack> item(String itemId) {
        Preconditions.requireText(itemId, "itemId");
        try {
            Object builder = getItemById.invoke(null, itemId);
            if (builder == null) {
                return Optional.empty();
            }
            Method build = builder.getClass().getMethod("build");
            build.setAccessible(true);
            Object built = build.invoke(builder);
            return built instanceof ItemStack itemStack ? Optional.of(itemStack) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> itemId(ItemStack itemStack) {
        Preconditions.requireNonNull(itemStack, "itemStack");
        try {
            Object value = getIdByItem.invoke(null, itemStack);
            return value instanceof String itemId && !itemId.isBlank() ? Optional.of(itemId) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return Optional.empty();
        }
    }

    public Class<?> oraxenItemsClass() {
        return oraxenItems;
    }
}
