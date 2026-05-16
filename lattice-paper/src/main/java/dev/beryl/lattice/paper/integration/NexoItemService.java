package dev.beryl.lattice.paper.integration;

public interface NexoItemService extends CustomItemProvider {
    @Override
    default String providerId() {
        return CustomItemRegistry.NEXO;
    }
}
