package dev.beryl.lattice.module;

import dev.beryl.lattice.util.Preconditions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModuleGraph {
    private final Map<ModuleId, LatticeModule> modules;
    private final List<LatticeModule> order;

    private ModuleGraph(Map<ModuleId, LatticeModule> modules, List<LatticeModule> order) {
        this.modules = Collections.unmodifiableMap(modules);
        this.order = List.copyOf(order);
    }

    public static ModuleGraph resolve(Collection<? extends LatticeModule> modules) {
        Preconditions.requireNonNull(modules, "modules");

        Map<ModuleId, LatticeModule> byId = new LinkedHashMap<>();
        for (LatticeModule module : modules) {
            ModuleId id = module.descriptor().id();
            LatticeModule previous = byId.putIfAbsent(id, module);
            if (previous != null) {
                throw new ModuleGraphException("Duplicate module id: " + id.value());
            }
        }

        List<LatticeModule> order = new ArrayList<>();
        Set<ModuleId> visited = new LinkedHashSet<>();
        Set<ModuleId> visiting = new LinkedHashSet<>();
        ArrayDeque<ModuleId> path = new ArrayDeque<>();

        for (ModuleId id : byId.keySet()) {
            visit(id, byId, visited, visiting, path, order);
        }

        return new ModuleGraph(byId, order);
    }

    private static void visit(
            ModuleId id,
            Map<ModuleId, LatticeModule> modules,
            Set<ModuleId> visited,
            Set<ModuleId> visiting,
            ArrayDeque<ModuleId> path,
            List<LatticeModule> order
    ) {
        if (visited.contains(id)) {
            return;
        }
        if (visiting.contains(id)) {
            path.addLast(id);
            throw new ModuleGraphException("Module dependency cycle: " + describe(path));
        }

        LatticeModule module = modules.get(id);
        if (module == null) {
            throw new ModuleGraphException("Missing module: " + id.value());
        }

        visiting.add(id);
        path.addLast(id);

        for (ModuleDependency dependency : module.descriptor().dependencies()) {
            if (!modules.containsKey(dependency.id())) {
                if (!dependency.optional()) {
                    throw new ModuleGraphException(
                            "Module " + id.value() + " requires missing module " + dependency.id().value()
                    );
                }
                continue;
            }
            visit(dependency.id(), modules, visited, visiting, path, order);
        }

        path.removeLast();
        visiting.remove(id);
        visited.add(id);
        order.add(module);
    }

    private static String describe(ArrayDeque<ModuleId> path) {
        return String.join(" -> ", path.stream().map(ModuleId::value).toList());
    }

    public List<LatticeModule> order() {
        return order;
    }

    public Map<ModuleId, LatticeModule> modules() {
        return modules;
    }
}

