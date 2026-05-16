package dev.beryl.lattice.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface JdbcRowMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
}
