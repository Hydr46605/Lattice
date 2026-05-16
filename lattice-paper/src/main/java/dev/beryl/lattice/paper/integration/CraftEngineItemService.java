package dev.beryl.lattice.paper.integration;

public interface CraftEngineItemService extends CustomItemProvider {
    @Override
    default String providerId() {
        return CustomItemRegistry.CRAFTENGINE;
    }
}
