package dev.beryl.lattice.paper.integration;

import dev.beryl.lattice.integration.IntegrationKey;

public final class PaperIntegrations {
    public static final IntegrationKey<JunctionVariableService> JUNCTION_VARIABLES =
            new IntegrationKey<>("junction-variables", JunctionVariableService.class);
    public static final IntegrationKey<PlaceholderApiService> PLACEHOLDER_API =
            new IntegrationKey<>("placeholderapi", PlaceholderApiService.class);
    public static final IntegrationKey<PacketEventsService> PACKET_EVENTS =
            new IntegrationKey<>("packetevents", PacketEventsService.class);
    public static final IntegrationKey<NexoItemService> NEXO_ITEMS =
            new IntegrationKey<>("nexo-items", NexoItemService.class);
    public static final IntegrationKey<OraxenItemService> ORAXEN_ITEMS =
            new IntegrationKey<>("oraxen-items", OraxenItemService.class);
    public static final IntegrationKey<ItemsAdderItemService> ITEMSADDER_ITEMS =
            new IntegrationKey<>("itemsadder-items", ItemsAdderItemService.class);
    public static final IntegrationKey<CraftEngineItemService> CRAFTENGINE_ITEMS =
            new IntegrationKey<>("craftengine-items", CraftEngineItemService.class);
    public static final IntegrationKey<CustomItemRegistry> CUSTOM_ITEM_REGISTRY =
            new IntegrationKey<>("custom-item-registry", CustomItemRegistry.class);

    private PaperIntegrations() {
    }
}
