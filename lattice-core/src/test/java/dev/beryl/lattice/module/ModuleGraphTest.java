package dev.beryl.lattice.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModuleGraphTest {
    @Test
    void ordersDependenciesBeforeDependents() {
        LatticeModule database = module("database");
        LatticeModule economy = module("economy", ModuleDependency.required("database"));

        ModuleGraph graph = ModuleGraph.resolve(List.of(economy, database));

        assertEquals(List.of(database, economy), graph.order());
    }

    @Test
    void rejectsCycles() {
        LatticeModule first = module("first", ModuleDependency.required("second"));
        LatticeModule second = module("second", ModuleDependency.required("first"));

        assertThrows(ModuleGraphException.class, () -> ModuleGraph.resolve(List.of(first, second)));
    }

    @Test
    void rejectsMissingRequiredDependencies() {
        LatticeModule module = module("shop", ModuleDependency.required("economy"));

        assertThrows(ModuleGraphException.class, () -> ModuleGraph.resolve(List.of(module)));
    }

    private static LatticeModule module(String id, ModuleDependency... dependencies) {
        return () -> new ModuleDescriptor(ModuleId.of(id), id, "test", Set.of(dependencies));
    }
}

