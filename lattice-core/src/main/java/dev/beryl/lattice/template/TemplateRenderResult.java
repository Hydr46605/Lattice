package dev.beryl.lattice.template;

import java.util.Set;

public record TemplateRenderResult(String output, Set<String> used, Set<String> unresolved) {
    public TemplateRenderResult {
        used = Set.copyOf(used);
        unresolved = Set.copyOf(unresolved);
    }

    public boolean successful() {
        return unresolved.isEmpty();
    }
}
