package dev.beryl.lattice.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class ReflectiveCustomItemProviderTest {
    @Test
    void bindsOraxenItemMethodsThroughReflection() throws Exception {
        ReflectiveOraxenItemService service = new ReflectiveOraxenItemService(
                FakeOraxenItems.class,
                FakeOraxenItems.class.getMethod("isAnItem", String.class),
                FakeOraxenItems.class.getMethod("getItemById", String.class),
                FakeOraxenItems.class.getMethod("getIdByItem", ItemStack.class)
        );

        assertEquals(CustomItemRegistry.ORAXEN, service.providerId());
        assertTrue(service.exists("menu_next"));
        assertFalse(service.exists("missing"));
        assertEquals(FakeOraxenItems.class, service.oraxenItemsClass());
        assertTrue(service.item("menu_next").isEmpty());
    }

    @Test
    void bindsItemsAdderItemMethodsThroughReflection() throws Exception {
        ReflectiveItemsAdderItemService service = new ReflectiveItemsAdderItemService(
                FakeCustomStack.class,
                FakeCustomStack.class.getMethod("isInRegistry", String.class),
                FakeCustomStack.class.getMethod("getInstance", String.class),
                FakeCustomStack.class.getMethod("byItemStack", ItemStack.class)
        );

        assertEquals(CustomItemRegistry.ITEMSADDER, service.providerId());
        assertTrue(service.exists("namespace:menu_next"));
        assertFalse(service.exists("missing"));
        assertEquals(FakeCustomStack.class, service.customStackClass());
        assertTrue(service.item("namespace:menu_next").isEmpty());
    }

    @Test
    void bindsCraftEngineItemMethodsThroughReflection() throws Exception {
        ReflectiveCraftEngineItemService service = new ReflectiveCraftEngineItemService(
                FakeCraftEngineItems.class,
                FakeCraftEngineItems.class.getMethod("byId", String.class),
                FakeCraftEngineItems.class.getMethod("getCustomItemId", ItemStack.class)
        );

        assertEquals(CustomItemRegistry.CRAFTENGINE, service.providerId());
        assertTrue(service.exists("default:menu_next"));
        assertFalse(service.exists("missing"));
        assertEquals(FakeCraftEngineItems.class, service.craftEngineItemsClass());
        assertTrue(service.item("default:menu_next").isEmpty());
    }

    @Test
    void registryNormalizesProviderIds() {
        CustomItemRegistry registry = new CustomItemRegistry();
        registry.register(new FakeProvider("Custom"));

        assertTrue(registry.hasProvider("custom"));
        assertTrue(registry.hasProvider("CUSTOM"));
        assertEquals(1, registry.providerIds().size());
    }

    public static final class FakeOraxenItems {
        public static boolean isAnItem(String itemId) {
            return "menu_next".equals(itemId);
        }

        public static FakeItemBuilder getItemById(String itemId) {
            return isAnItem(itemId) ? new FakeItemBuilder() : null;
        }

        public static String getIdByItem(ItemStack itemStack) {
            return null;
        }
    }

    public static final class FakeCustomStack {
        public static boolean isInRegistry(String itemId) {
            return "namespace:menu_next".equals(itemId);
        }

        public static FakeCustomStack getInstance(String itemId) {
            return isInRegistry(itemId) ? new FakeCustomStack() : null;
        }

        public static FakeCustomStack byItemStack(ItemStack itemStack) {
            return null;
        }

        public Object getItemStack() {
            return new Object();
        }

        public String getNamespacedID() {
            return "namespace:menu_next";
        }
    }

    public static final class FakeCraftEngineItems {
        public static FakeCraftEngineDefinition byId(String itemId) {
            return "default:menu_next".equals(itemId) ? new FakeCraftEngineDefinition() : null;
        }

        public static FakeCraftEngineKey getCustomItemId(ItemStack itemStack) {
            return null;
        }
    }

    public static final class FakeCraftEngineDefinition {
        public Object buildBukkitItem() {
            return new Object();
        }
    }

    public static final class FakeCraftEngineKey {
        public String asString() {
            return "default:menu_next";
        }
    }

    public static final class FakeItemBuilder {
        public Object build() {
            return new Object();
        }
    }

    private record FakeProvider(String providerId) implements CustomItemProvider {
        @Override
        public boolean exists(String itemId) {
            return false;
        }

        @Override
        public java.util.Optional<ItemStack> item(String itemId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> itemId(ItemStack itemStack) {
            return java.util.Optional.empty();
        }
    }
}
