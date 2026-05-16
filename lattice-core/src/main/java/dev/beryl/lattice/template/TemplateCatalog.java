package dev.beryl.lattice.template;

import java.util.Optional;

public interface TemplateCatalog {
    Optional<PluginTemplate> plugin(String id);

    Optional<ModuleTemplate> module(String id);
}

