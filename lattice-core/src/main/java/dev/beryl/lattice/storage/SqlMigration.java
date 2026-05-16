package dev.beryl.lattice.storage;

import dev.beryl.lattice.util.Preconditions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.List;

public record SqlMigration(String id, int order, List<String> statements) implements Migration {
    public SqlMigration {
        id = Preconditions.requireText(id, "id");
        statements = List.copyOf(statements);
        Preconditions.checkArgument(!statements.isEmpty(), "Migration statements cannot be empty");
    }

    public static SqlMigration of(String id, int order, String statement) {
        return new SqlMigration(id, order, List.of(statement));
    }

    @Override
    public String checksum() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(id.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            digest.update(Integer.toString(order).getBytes(StandardCharsets.UTF_8));
            for (String statement : statements) {
                digest.update((byte) '\n');
                digest.update(statement.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    @Override
    public void apply(StorageConnection connection) throws StorageException {
        if (!(connection instanceof JdbcStorageConnection jdbc)) {
            throw new StorageException("SqlMigration requires a JdbcStorageConnection");
        }
        jdbc.useConnection(sqlConnection -> {
            try (Statement statement = sqlConnection.createStatement()) {
                for (String sql : statements) {
                    statement.executeUpdate(sql);
                }
            }
            return null;
        });
    }
}
