package dev.beryl.lattice.config;

import java.util.List;

public record ReloadResult<T>(T value, boolean successful, List<String> problems) {
    public ReloadResult {
        problems = problems == null ? List.of() : List.copyOf(problems);
    }
}

