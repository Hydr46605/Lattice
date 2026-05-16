package dev.beryl.lattice.ui.config;

import dev.beryl.lattice.util.Preconditions;
import java.util.Map;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record ConfiguredUiAction(String type, Map<String, String> data) {
    public ConfiguredUiAction {
        type = Preconditions.requireText(type, "type");
        data = Map.copyOf(data == null ? Map.of() : data);
    }

    public String value(String key) {
        return data.getOrDefault(Preconditions.requireText(key, "key"), "");
    }
}
