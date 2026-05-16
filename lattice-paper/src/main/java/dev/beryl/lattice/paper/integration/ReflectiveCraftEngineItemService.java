package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

@InternalApi
public final class ReflectiveCraftEngineItemService implements CraftEngineItemService {
    private final Class<?> craftEngineItems;
    private final Method byId;
    private final Method getCustomItemId;

    public ReflectiveCraftEngineItemService(Class<?> craftEngineItems, Method byId, Method getCustomItemId) {
        this.craftEngineItems = Preconditions.requireNonNull(craftEngineItems, "craftEngineItems");
        this.byId = Preconditions.requireNonNull(byId, "byId");
        this.getCustomItemId = Preconditions.requireNonNull(getCustomItemId, "getCustomItemId");
        this.byId.setAccessible(true);
        this.getCustomItemId.setAccessible(true);
    }

    @Override
    public boolean exists(String itemId) {
        return definition(itemId).isPresent();
    }

    @Override
    public Optional<ItemStack> item(String itemId) {
        return definition(itemId).flatMap(this::buildBukkitItem);
    }

    @Override
    public Optional<String> itemId(ItemStack itemStack) {
        Preconditions.requireNonNull(itemStack, "itemStack");
        try {
            Object key = getCustomItemId.invoke(null, itemStack);
            if (key == null) {
                return Optional.empty();
            }
            return Optional.of(keyString(key));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return Optional.empty();
        }
    }

    private Optional<Object> definition(String itemId) {
        Preconditions.requireText(itemId, "itemId");
        try {
            return Optional.ofNullable(byId.invoke(null, itemId));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return Optional.empty();
        }
    }

    private Optional<ItemStack> buildBukkitItem(Object definition) {
        try {
            Method buildBukkitItem = definition.getClass().getMethod("buildBukkitItem");
            buildBukkitItem.setAccessible(true);
            Object item = buildBukkitItem.invoke(definition);
            return item instanceof ItemStack itemStack ? Optional.of(itemStack) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    private String keyString(Object key) {
        try {
            Method asString = key.getClass().getMethod("asString");
            asString.setAccessible(true);
            Object value = asString.invoke(key);
            if (value instanceof String itemId && !itemId.isBlank()) {
                return itemId;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
            // Fall back to CraftEngine Key#toString().
        }
        return key.toString();
    }

    public Class<?> craftEngineItemsClass() {
        return craftEngineItems;
    }
}
