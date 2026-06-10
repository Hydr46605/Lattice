package dev.beryl.lattice.module;

import dev.beryl.lattice.api.InternalApi;

@InternalApi
public final class ModuleLifecycleException extends Exception {
    private final String operation;
    private final ModuleId moduleId;

    ModuleLifecycleException(String operation, ModuleId moduleId, Throwable cause) {
        super("Module " + moduleId.value() + " failed during " + operation, cause);
        this.operation = operation;
        this.moduleId = moduleId;
    }

    public String operation() {
        return operation;
    }

    public ModuleId moduleId() {
        return moduleId;
    }
}
