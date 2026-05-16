package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

@InternalApi
public final class ReflectiveNexoItemService implements NexoItemService {
    private final Class<?> nexoItems;
    private final Method exists;
    private final Method itemFromId;
    private final Method idFromItem;

    public ReflectiveNexoItemService(Class<?> nexoItems, Method exists, Method itemFromId, Method idFromItem) {
        this.nexoItems = Preconditions.requireNonNull(nexoItems, "nexoItems");
        this.exists = Preconditions.requireNonNull(exists, "exists");
        this.itemFromId = Preconditions.requireNonNull(itemFromId, "itemFromId");
        this.idFromItem = Preconditions.requireNonNull(idFromItem, "idFromItem");
        this.exists.setAccessible(true);
        this.itemFromId.setAccessible(true);
        this.idFromItem.setAccessible(true);
    }

    @Override
    public boolean exists(String itemId) {
        Preconditions.requireText(itemId, "itemId");
        try {
            return Boolean.TRUE.equals(exists.invoke(null, itemId));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return false;
        }
    }

    @Override
    public Optional<ItemStack> item(String itemId) {
        Preconditions.requireText(itemId, "itemId");
        try {
            Object builder = itemFromId.invoke(null, itemId);
            if (builder == null) {
                return Optional.empty();
            }
            Method build = builder.getClass().getMethod("build");
            build.setAccessible(true);
            Object built = build.invoke(builder);
            if (!(built instanceof ItemStack itemStack)) {
                return Optional.empty();
            }
            return Optional.of(itemStack);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> itemId(ItemStack itemStack) {
        Preconditions.requireNonNull(itemStack, "itemStack");
        try {
            Object value = idFromItem.invoke(null, itemStack);
            return value instanceof String itemId && !itemId.isBlank() ? Optional.of(itemId) : Optional.empty();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return Optional.empty();
        }
    }

    public Class<?> nexoItemsClass() {
        return nexoItems;
    }
}
