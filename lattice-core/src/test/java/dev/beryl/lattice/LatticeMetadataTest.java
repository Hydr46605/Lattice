package dev.beryl.lattice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LatticeMetadataTest {
    @Test
    void publicVersionMatchesGradleProperty() throws IOException {
        assertEquals(gradleVersion(), Lattice.VERSION);
    }

    private String gradleVersion() throws IOException {
        return Files.readAllLines(repositoryRoot().resolve("gradle.properties")).stream()
                .filter(line -> line.startsWith("latticeVersion="))
                .findFirst()
                .map(line -> line.substring("latticeVersion=".length()))
                .orElseThrow(() -> new AssertionError("Missing latticeVersion in gradle.properties"));
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(current.resolve("gradle.properties"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isRegularFile(parent.resolve("gradle.properties"))) {
            return parent;
        }
        throw new AssertionError("Cannot locate Lattice repository root from " + current);
    }
}
