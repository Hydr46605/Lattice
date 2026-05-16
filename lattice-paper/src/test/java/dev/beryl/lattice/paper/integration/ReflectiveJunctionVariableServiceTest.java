package dev.beryl.lattice.paper.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReflectiveJunctionVariableServiceTest {
    @Test
    void bindsJunctionVariableMethodsThroughReflection() throws Exception {
        FakeJunctionPlugin junction = new FakeJunctionPlugin();
        ReflectiveJunctionVariableService service = new ReflectiveJunctionVariableService(
                junction,
                FakeJunctionPlugin.class.getMethod("resolveVariable", String.class),
                FakeJunctionPlugin.class.getMethod("variablesSnapshot")
        );

        assertEquals(Optional.of("BerylCraft"), service.resolveVariable("server"));
        assertTrue(service.resolveVariable("missing").isEmpty());
        assertEquals(Map.of("server", "BerylCraft", "discord", "discord.gg/example"), service.variablesSnapshot());
    }

    public static final class FakeJunctionPlugin {
        public Optional<String> resolveVariable(String key) {
            return Optional.ofNullable(variablesSnapshot().get(key));
        }

        public Map<String, String> variablesSnapshot() {
            return Map.of("server", "BerylCraft", "discord", "discord.gg/example");
        }
    }
}
