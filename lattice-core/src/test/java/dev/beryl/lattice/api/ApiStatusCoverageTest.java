package dev.beryl.lattice.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApiStatusCoverageTest {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern PUBLIC_TYPE_PATTERN = Pattern.compile(
            "(?m)^\\s*public\\s+(?:(?:final|abstract|sealed|non-sealed)\\s+)*(?:@interface|class|interface|record|enum)\\s+([\\w$]+)"
    );

    @Test
    void everyPublicMainTypeHasApiStatus() throws Exception {
        List<ApiType> types = publicTypes();

        List<String> missing = types.stream()
                .filter(type -> type.statusOptional().isEmpty())
                .map(ApiType::display)
                .sorted()
                .toList();

        assertTrue(missing.isEmpty(), "Public main types without API status:\n" + String.join("\n", missing));
    }

    @Test
    void experimentalAndInternalAreasStayClearlyLabelled() throws Exception {
        List<ApiType> types = publicTypes();

        assertPackageStatus(types, "dev.beryl.lattice.template", ApiStatus.EXPERIMENTAL);
        assertPackageStatus(types, "dev.beryl.lattice.template.annotation", ApiStatus.EXPERIMENTAL);
        assertPackageStatus(types, "dev.beryl.lattice.hook", ApiStatus.EXPERIMENTAL);
        assertPackageStatus(types, "dev.beryl.lattice.paper.ui", ApiStatus.INTERNAL);

        assertTypeStatus(types, "dev.beryl.lattice.diagnostics.RuntimeDiagnosticContributor", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.diagnostics.PaperDiagnosticContributor", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.diagnostics.PaperDiagnostics", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.hook.PaperPluginHookService", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.integration.PaperIntegrationBootstrap", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.integration.PaperIntegrationProbe", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.integration.ReflectiveCraftEngineItemService", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.integration.ReflectiveItemsAdderItemService", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.integration.ReflectiveNexoItemService", ApiStatus.INTERNAL);
        assertTypeStatus(types, "dev.beryl.lattice.paper.integration.ReflectiveOraxenItemService", ApiStatus.INTERNAL);
    }

    @Test
    void stableAuthoringEntryPointsStayStable() throws Exception {
        List<ApiType> types = publicTypes();

        assertTypeStatus(types, "dev.beryl.lattice.command.CommandExceptionMappers", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.lifecycle.LatticeBuilder", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.lifecycle.LatticeRuntime", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.bootstrap.LatticePaper", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.bootstrap.LatticeHost", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.bootstrap.LatticeHostProvider", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.bootstrap.LatticePaperPlugin", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.bootstrap.LatticePluginHandle", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.bootstrap.PaperServices", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.bootstrap.StandaloneLatticeBootstrap", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.diagnostics.PaperDiagnosticRenderer", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.integration.PaperIntegrations", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.storage.PaperAsyncStorageRunner", ApiStatus.STABLE);
        assertTypeStatus(types, "dev.beryl.lattice.paper.storage.PaperStorageDefaults", ApiStatus.STABLE);
    }

    private static void assertPackageStatus(List<ApiType> types, String packageName, ApiStatus expected) {
        List<ApiType> packageTypes = types.stream()
                .filter(type -> type.packageName().equals(packageName))
                .toList();
        assertTrue(!packageTypes.isEmpty(), "No public types found in " + packageName);
        for (ApiType type : packageTypes) {
            assertEquals(expected, type.statusOptional().orElseThrow(), type.display());
        }
    }

    private static void assertTypeStatus(List<ApiType> types, String qualifiedName, ApiStatus expected) {
        ApiType type = types.stream()
                .filter(candidate -> candidate.qualifiedName().equals(qualifiedName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No public type found: " + qualifiedName));

        assertEquals(expected, type.statusOptional().orElseThrow(), type.display());
    }

    private static List<ApiType> publicTypes() throws IOException {
        Path root = repositoryRoot();
        List<Path> sourceRoots = List.of(
                root.resolve("lattice-api/src/main/java"),
                root.resolve("lattice-core/src/main/java"),
                root.resolve("lattice-paper/src/main/java")
        );

        List<ApiType> types = new ArrayList<>();
        for (Path sourceRoot : sourceRoots) {
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                for (Path source : paths
                        .filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                        .sorted(Comparator.comparing(Path::toString))
                        .toList()) {
                    parsePublicType(sourceRoot, source).ifPresent(types::add);
                }
            }
        }
        return List.copyOf(types);
    }

    private static Optional<ApiType> parsePublicType(Path sourceRoot, Path source) {
        try {
            String text = Files.readString(source);
            Matcher type = PUBLIC_TYPE_PATTERN.matcher(text);
            if (!type.find()) {
                return Optional.empty();
            }

            Matcher packageMatcher = PACKAGE_PATTERN.matcher(text);
            if (!packageMatcher.find()) {
                throw new AssertionError("Missing package declaration in " + source);
            }

            String packageName = packageMatcher.group(1);
            String typeName = type.group(1);
            ApiStatus packageStatus = packageStatus(source.getParent().resolve("package-info.java"));
            ApiStatus typeStatus = statusIn(text.substring(packageMatcher.end(), type.start()), source);
            return Optional.of(new ApiType(
                    sourceRoot.relativize(source),
                    packageName,
                    typeName,
                    typeStatus == null ? packageStatus : typeStatus
            ));
        } catch (IOException exception) {
            throw new AssertionError("Failed to read source " + source, exception);
        }
    }

    private static ApiStatus packageStatus(Path packageInfo) throws IOException {
        if (!Files.exists(packageInfo)) {
            return null;
        }
        return statusIn(Files.readString(packageInfo), packageInfo);
    }

    private static ApiStatus statusIn(String text, Path source) {
        List<ApiStatus> matches = new ArrayList<>();
        for (ApiStatus status : ApiStatus.values()) {
            if (Pattern.compile("@(?:[\\w$]+\\.)*" + status.annotationName() + "\\b").matcher(text).find()) {
                matches.add(status);
            }
        }
        if (matches.size() > 1) {
            throw new AssertionError("Multiple API status annotations in " + source + ": " + matches);
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("lattice-core")) && Files.isDirectory(current.resolve("lattice-paper"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("lattice-core")) && Files.isDirectory(parent.resolve("lattice-paper"))) {
            return parent;
        }
        throw new AssertionError("Cannot locate Lattice repository root from " + current);
    }

    private enum ApiStatus {
        STABLE("StableApi"),
        EXPERIMENTAL("ExperimentalApi"),
        INTERNAL("InternalApi");

        private final String annotationName;

        ApiStatus(String annotationName) {
            this.annotationName = annotationName;
        }

        String annotationName() {
            return annotationName;
        }
    }

    private record ApiType(Path source, String packageName, String typeName, ApiStatus status) {
        String qualifiedName() {
            return packageName + "." + typeName;
        }

        Optional<ApiStatus> statusOptional() {
            return Optional.ofNullable(status);
        }

        String display() {
            return qualifiedName() + " (" + source + ")";
        }
    }
}
