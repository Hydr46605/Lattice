package dev.beryl.lattice.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

public class ReflectiveNexoItemServiceTest {
    @Test
    void bindsNexoItemMethodsThroughReflection() throws Exception {
        ReflectiveNexoItemService service = new ReflectiveNexoItemService(
                FakeNexoItems.class,
                FakeNexoItems.class.getMethod("exists", String.class),
                FakeNexoItems.class.getMethod("itemFromId", String.class),
                FakeNexoItems.class.getMethod("idFromItem", ItemStack.class)
        );

        assertTrue(service.exists("menu_next"));
        assertFalse(service.exists("missing"));
        assertEquals(CustomItemRegistry.NEXO, service.providerId());
        assertEquals(FakeNexoItems.class, service.nexoItemsClass());
        assertTrue(service.item("menu_next").isEmpty());
        assertTrue(service.item("missing").isEmpty());
    }

    public static final class FakeNexoItems {
        public static boolean exists(String itemId) {
            return "menu_next".equals(itemId);
        }

        public static FakeItemBuilder itemFromId(String itemId) {
            return exists(itemId) ? new FakeItemBuilder() : null;
        }

        public static String idFromItem(ItemStack itemStack) {
            return null;
        }
    }

    public static final class FakeItemBuilder {
        public Object build() {
            return new Object();
        }
    }
}
