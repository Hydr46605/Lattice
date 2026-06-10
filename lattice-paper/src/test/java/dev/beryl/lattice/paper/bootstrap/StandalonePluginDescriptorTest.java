package dev.beryl.lattice.paper.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandalonePluginDescriptorTest {
    private static final List<String> EXPECTED_OPTIONAL_DEPENDENCIES = List.of(
            "PlaceholderAPI",
            "Nexo",
            "Oraxen",
            "ItemsAdder",
            "CraftEngine",
            "PacketEvents");

    @Test
    void descriptorListsOnlyIntentionalOptionalDependencies() throws Exception {
        String descriptorPath = System.getProperty("lattice.standalonePluginDescriptor");
        assertNotNull(descriptorPath, "lattice.standalonePluginDescriptor system property must be set");

        String descriptor = Files.readString(Path.of(descriptorPath));

        assertEquals(EXPECTED_OPTIONAL_DEPENDENCIES, standaloneServerDependencyNames(descriptor));
        EXPECTED_OPTIONAL_DEPENDENCIES.forEach(pluginName -> assertOptionalDependency(descriptor, pluginName));
        assertFalse(descriptor.contains("Junction:"), "Junction is detected reflectively and is not a standalone Paper dependency");
    }

    private List<String> standaloneServerDependencyNames(String descriptor) {
        List<String> pluginNames = new ArrayList<>();
        boolean inDependencies = false;
        boolean inServer = false;

        for (String line : descriptor.split("\\R")) {
            if (line.equals("dependencies:")) {
                inDependencies = true;
                continue;
            }
            if (!inDependencies) {
                continue;
            }
            if (line.equals("  server:")) {
                inServer = true;
                continue;
            }
            if (!inServer) {
                continue;
            }
            if (!line.startsWith("    ")) {
                break;
            }
            if (line.startsWith("    ") && !line.startsWith("      ") && line.endsWith(":")) {
                pluginNames.add(line.trim().replace(":", ""));
            }
        }

        return pluginNames;
    }

    private void assertOptionalDependency(String descriptor, String pluginName) {
        String block = pluginName + ":\n"
                + "      load: BEFORE\n"
                + "      required: false\n"
                + "      join-classpath: true";
        assertTrue(descriptor.contains(block), "Missing optional dependency block for " + pluginName);
    }
}
