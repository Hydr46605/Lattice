package dev.beryl.lattice.paper.integration;

public interface OraxenItemService extends CustomItemProvider {
    @Override
    default String providerId() {
        return CustomItemRegistry.ORAXEN;
    }
}
