package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.api.InternalApi;
import dev.beryl.lattice.integration.IntegrationManager;
import dev.beryl.lattice.integration.SimpleIntegration;
import dev.beryl.lattice.util.Preconditions;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@InternalApi
public final class PaperIntegrationBootstrap {
    private PaperIntegrationBootstrap() {
    }

    public static void registerDefaults(JavaPlugin plugin, IntegrationManager integrations) {
        Preconditions.requireNonNull(plugin, "plugin");
        Preconditions.requireNonNull(integrations, "integrations");
        integrations.register(junctionVariables(plugin));
        integrations.register(placeholderApi(plugin));
        integrations.register(packetEvents(plugin));
        integrations.register(nexoItems(plugin));
        integrations.register(oraxenItems(plugin));
        integrations.register(itemsAdderItems(plugin));
        integrations.register(craftEngineItems(plugin));
        integrations.register(customItemRegistry(integrations));
    }

    private static SimpleIntegration<JunctionVariableService> junctionVariables(JavaPlugin plugin) {
        Plugin junction = plugin.getServer().getPluginManager().getPlugin("Junction");
        if (junction == null) {
            return SimpleIntegration.missing(PaperIntegrations.JUNCTION_VARIABLES);
        }
        try {
            Method resolveVariable = junction.getClass().getMethod("resolveVariable", String.class);
            Method variablesSnapshot = junction.getClass().getMethod("variablesSnapshot");
            return SimpleIntegration.available(
                    PaperIntegrations.JUNCTION_VARIABLES,
                    new ReflectiveJunctionVariableService(junction, resolveVariable, variablesSnapshot)
            );
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Junction is loaded but Lattice could not bind to its variable API: {0}",
                    failureMessage(exception));
            return SimpleIntegration.failed(PaperIntegrations.JUNCTION_VARIABLES, failureDetails(exception));
        }
    }

    private static SimpleIntegration<PlaceholderApiService> placeholderApi(JavaPlugin plugin) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return SimpleIntegration.missing(PaperIntegrations.PLACEHOLDER_API);
        }
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return SimpleIntegration.available(
                    PaperIntegrations.PLACEHOLDER_API,
                    new PapiPlaceholderApiService(plugin)
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().log(Level.WARNING,
                    "PlaceholderAPI is enabled but Lattice could not bind to it: {0}",
                    failureMessage(exception));
            return SimpleIntegration.failed(PaperIntegrations.PLACEHOLDER_API, failureDetails(exception));
        }
    }

    private static SimpleIntegration<PacketEventsService> packetEvents(JavaPlugin plugin) {
        if (!isAnyPluginEnabled(plugin.getServer().getPluginManager(), "PacketEvents", "packetevents")) {
            return SimpleIntegration.missing(PaperIntegrations.PACKET_EVENTS);
        }
        try {
            Class<?> packetEvents = Class.forName("com.github.retrooper.packetevents.PacketEvents");
            Method getApi = packetEvents.getMethod("getAPI");
            Object api = getApi.invoke(null);
            if (api == null) {
                return SimpleIntegration.failed(
                        PaperIntegrations.PACKET_EVENTS,
                        Map.of("reason", "PacketEvents.getAPI returned null")
                );
            }
            ReflectivePacketEventsService service = new ReflectivePacketEventsService(api, plugin);
            return SimpleIntegration.available(
                    PaperIntegrations.PACKET_EVENTS,
                    service,
                    integrationDetails(service)
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().log(Level.WARNING,
                    "PacketEvents is enabled but Lattice could not bind to it: {0}",
                    failureMessage(exception));
            return SimpleIntegration.failed(PaperIntegrations.PACKET_EVENTS, failureDetails(exception));
        }
    }

    private static SimpleIntegration<NexoItemService> nexoItems(JavaPlugin plugin) {
        if (!isAnyPluginEnabled(plugin.getServer().getPluginManager(), "Nexo", "nexo")) {
            return SimpleIntegration.missing(PaperIntegrations.NEXO_ITEMS);
        }
        try {
            Class<?> nexoItems = Class.forName("com.nexomc.nexo.api.NexoItems");
            Method exists = nexoItems.getMethod("exists", String.class);
            Method itemFromId = nexoItems.getMethod("itemFromId", String.class);
            Method idFromItem = nexoItems.getMethod("idFromItem", org.bukkit.inventory.ItemStack.class);
            return SimpleIntegration.available(
                    PaperIntegrations.NEXO_ITEMS,
                    new ReflectiveNexoItemService(nexoItems, exists, itemFromId, idFromItem)
            );
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Nexo is enabled but Lattice could not bind to its item API: {0}",
                    failureMessage(exception));
            return SimpleIntegration.failed(PaperIntegrations.NEXO_ITEMS, failureDetails(exception));
        }
    }

    private static SimpleIntegration<OraxenItemService> oraxenItems(JavaPlugin plugin) {
        if (!isAnyPluginEnabled(plugin.getServer().getPluginManager(), "Oraxen", "oraxen")) {
            return SimpleIntegration.missing(PaperIntegrations.ORAXEN_ITEMS);
        }
        try {
            Class<?> oraxenItems = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            Method isAnItem = oraxenItems.getMethod("isAnItem", String.class);
            Method getItemById = oraxenItems.getMethod("getItemById", String.class);
            Method getIdByItem = oraxenItems.getMethod("getIdByItem", org.bukkit.inventory.ItemStack.class);
            return SimpleIntegration.available(
                    PaperIntegrations.ORAXEN_ITEMS,
                    new ReflectiveOraxenItemService(oraxenItems, isAnItem, getItemById, getIdByItem)
            );
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Oraxen is enabled but Lattice could not bind to its item API: {0}",
                    failureMessage(exception));
            return SimpleIntegration.failed(PaperIntegrations.ORAXEN_ITEMS, failureDetails(exception));
        }
    }

    private static SimpleIntegration<ItemsAdderItemService> itemsAdderItems(JavaPlugin plugin) {
        if (!isAnyPluginEnabled(plugin.getServer().getPluginManager(), "ItemsAdder", "itemsadder")) {
            return SimpleIntegration.missing(PaperIntegrations.ITEMSADDER_ITEMS);
        }
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Method isInRegistry = customStack.getMethod("isInRegistry", String.class);
            Method getInstance = customStack.getMethod("getInstance", String.class);
            Method byItemStack = customStack.getMethod("byItemStack", org.bukkit.inventory.ItemStack.class);
            return SimpleIntegration.available(
                    PaperIntegrations.ITEMSADDER_ITEMS,
                    new ReflectiveItemsAdderItemService(customStack, isInRegistry, getInstance, byItemStack)
            );
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "ItemsAdder is enabled but Lattice could not bind to its item API: {0}",
                    failureMessage(exception));
            return SimpleIntegration.failed(PaperIntegrations.ITEMSADDER_ITEMS, failureDetails(exception));
        }
    }

    private static SimpleIntegration<CraftEngineItemService> craftEngineItems(JavaPlugin plugin) {
        if (!isAnyPluginEnabled(plugin.getServer().getPluginManager(), "CraftEngine", "craftengine")) {
            return SimpleIntegration.missing(PaperIntegrations.CRAFTENGINE_ITEMS);
        }
        try {
            Class<?> craftEngineItems = Class.forName("net.momirealms.craftengine.bukkit.api.CraftEngineItems");
            Method byId = craftEngineItems.getMethod("byId", String.class);
            Method getCustomItemId = craftEngineItems.getMethod("getCustomItemId", org.bukkit.inventory.ItemStack.class);
            return SimpleIntegration.available(
                    PaperIntegrations.CRAFTENGINE_ITEMS,
                    new ReflectiveCraftEngineItemService(craftEngineItems, byId, getCustomItemId)
            );
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING,
                    "CraftEngine is enabled but Lattice could not bind to its item API: {0}",
                    failureMessage(exception));
            return SimpleIntegration.failed(PaperIntegrations.CRAFTENGINE_ITEMS, failureDetails(exception));
        }
    }

    private static SimpleIntegration<CustomItemRegistry> customItemRegistry(IntegrationManager integrations) {
        CustomItemRegistry registry = new CustomItemRegistry();
        integrations.service(PaperIntegrations.NEXO_ITEMS).ifPresent(registry::register);
        integrations.service(PaperIntegrations.ORAXEN_ITEMS).ifPresent(registry::register);
        integrations.service(PaperIntegrations.ITEMSADDER_ITEMS).ifPresent(registry::register);
        integrations.service(PaperIntegrations.CRAFTENGINE_ITEMS).ifPresent(registry::register);
        return SimpleIntegration.available(PaperIntegrations.CUSTOM_ITEM_REGISTRY, registry);
    }

    private static boolean isAnyPluginEnabled(PluginManager pluginManager, String... names) {
        for (String name : names) {
            if (pluginManager.isPluginEnabled(name)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> integrationDetails(PacketEventsService service) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("apiClass", service.apiClassName());
        service.version().ifPresent(version -> details.put("version", version));
        return details;
    }

    static Map<String, String> failureDetails(Throwable exception) {
        Preconditions.requireNonNull(exception, "exception");
        return Map.of(
                "reason", exception.getClass().getName(),
                "message", exception.getMessage() == null ? "" : exception.getMessage()
        );
    }

    static String failureMessage(Throwable exception) {
        Preconditions.requireNonNull(exception, "exception");
        return exception.getClass().getSimpleName()
                + (exception.getMessage() == null ? "" : ": " + exception.getMessage());
    }
}
