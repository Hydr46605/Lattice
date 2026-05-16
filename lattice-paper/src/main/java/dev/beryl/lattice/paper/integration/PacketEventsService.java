package dev.beryl.lattice.paper.integration;

import java.util.Optional;

public interface PacketEventsService {
    Object api();

    default String apiClassName() {
        return api().getClass().getName();
    }

    default Optional<String> version() {
        Package apiPackage = api().getClass().getPackage();
        return apiPackage == null ? Optional.empty() : Optional.ofNullable(apiPackage.getImplementationVersion());
    }
}
