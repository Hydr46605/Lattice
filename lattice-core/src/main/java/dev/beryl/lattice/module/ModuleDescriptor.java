package dev.beryl.lattice.module;

import dev.beryl.lattice.util.Preconditions;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record ModuleDescriptor(
        ModuleId id,
        String name,
        String version,
        Set<ModuleDependency> dependencies
) {
    public ModuleDescriptor {
        id = Preconditions.requireNonNull(id, "id");
        name = name == null || name.isBlank() ? id.value() : name;
        version = version == null || version.isBlank() ? "unspecified" : version;
        dependencies = Collections.unmodifiableSet(new LinkedHashSet<>(
                dependencies == null ? Set.of() : dependencies
        ));
    }

    public static ModuleDescriptor of(String id) {
        return new ModuleDescriptor(ModuleId.of(id), id, "unspecified", Set.of());
    }
}

