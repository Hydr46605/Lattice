package dev.beryl.lattice.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SharedStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void reusesPoolForMatchingStorageConfigUntilLastConnectionCloses() throws Exception {
        SharedDataSourceManager dataSources = new SharedDataSourceManager();
        SharedStorageService storage = SharedStorageService.withJdbcDefaults(dataSources);
        StorageConfig config = StorageConfig.sqlite(tempDir.resolve("shared.db"));

        StorageConnection first = storage.connect(config);
        StorageConnection second = storage.connect(config);

        assertEquals(1, dataSources.activePools());

        first.close();
        assertEquals(1, dataSources.activePools());

        second.close();
        assertEquals(0, dataSources.activePools());
    }

    @Test
    void keepsDifferentSqliteFilesInDifferentPools() throws Exception {
        SharedDataSourceManager dataSources = new SharedDataSourceManager();
        SharedStorageService storage = SharedStorageService.withJdbcDefaults(dataSources);

        try (StorageConnection first = storage.connect(StorageConfig.sqlite(tempDir.resolve("first.db")));
             StorageConnection second = storage.connect(StorageConfig.sqlite(tempDir.resolve("second.db")))) {
            assertEquals(2, dataSources.activePools());
            assertTrue(storage.diagnostics().connectionHealth().size() >= 2);
        }
    }
}
