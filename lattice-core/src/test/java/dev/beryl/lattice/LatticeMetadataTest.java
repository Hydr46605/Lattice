package dev.beryl.lattice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class LatticeMetadataTest {
    @Test
    void publicVersionMatchesGradleProjectVersion() {
        String projectVersion = System.getProperty("lattice.test.projectVersion");

        assertNotNull(projectVersion, "Gradle must provide lattice.test.projectVersion to metadata tests");
        assertEquals(projectVersion, Lattice.VERSION);
    }
}
