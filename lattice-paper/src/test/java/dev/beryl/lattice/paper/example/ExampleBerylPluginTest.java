package dev.beryl.lattice.paper.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExampleBerylPluginTest {
    @Test
    void exampleConfigHasUsableDefaults() {
        ExampleBerylPlugin.ExampleConfig defaults = ExampleBerylPlugin.ExampleConfig.defaults();

        assertEquals(30, defaults.heartbeatSeconds());
        assertTrue(defaults.enabledFeatures().contains("commands"));
    }
}

