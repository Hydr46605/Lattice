package dev.beryl.lattice.paper.bootstrap;

import dev.beryl.lattice.util.Preconditions;
import java.util.Optional;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class LatticeHostProvider {
    private LatticeHostProvider() {
    }

    public static Optional<LatticeHost> find(JavaPlugin plugin) {
        Preconditions.requireNonNull(plugin, "plugin");
        RegisteredServiceProvider<LatticeHost> registration = plugin.getServer()
                .getServicesManager()
                .getRegistration(LatticeHost.class);
        if (registration == null || registration.getPlugin().equals(plugin)) {
            return Optional.empty();
        }
        return Optional.of(registration.getProvider());
    }
}
