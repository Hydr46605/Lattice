package dev.beryl.lattice.paper.integration;

public interface ItemsAdderItemService extends CustomItemProvider {
    @Override
    default String providerId() {
        return CustomItemRegistry.ITEMSADDER;
    }
}
