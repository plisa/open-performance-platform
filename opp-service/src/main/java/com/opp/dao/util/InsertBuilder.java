package com.opp.dao.util;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

/**
 * Helper class to build insert statements.
 *
 * Created by ctobe on 7/13/16.
 */
public class InsertBuilder {

    protected final String table;
    protected final Map<String,Object> columns = new LinkedHashMap<>();

    // Columns to update on a duplicate row if this behavior is enabled
    private Optional<Map<String,Object>> onDuplicateUpdateColumns = Optional.empty();

    // Whether or not to insert values if they are null
    private boolean excludeNullValues = true;

    public static InsertBuilder insertInto(String table) {
        return new InsertBuilder(table);
    }

    protected InsertBuilder(String table) {
        this.table = table;
    }

    /**
     * Insert the provided value into the provided column.
     * @param column
     * @param value
     * @return
     */
    public InsertBuilder value(String column, Object value) {
        if (shouldInsertValue(value, excludeNullValues)) {
            columns.put(column, value);
        }
        return this;
    }

    /**
     * Insert the provided value into the provided column. Value will not be included in the insert if
     * <code>excludeIfNull</code> is set to <code>true</code> and <code>value</code> is <code>null</code>.
     * @param column
     * @param value
     * @param excludeIfNull
     * @return
     */
    public InsertBuilder value(String column, Object value, boolean excludeIfNull) {
        if (shouldInsertValue(value, excludeIfNull)) {
            columns.put(column, value);
        }
        return this;
    }

    /**
     * Indicates the columns that should be updated if thre is a duplicate row encountered. The update values are taken
     * from those provided in the value calls and follow the same null exclusion behavior.
     *
     * @param columnsToUpdate the columns to update with the otherwise inserted values
     * @return
     */
    public InsertBuilder onDuplicateUpdate(String ...columnsToUpdate) {
        Set<String> columnNames = new HashSet<>(asList(columnsToUpdate));
        // remove any columns that do not exist or were filtered out due to null value exclusion
        columnNames.retainAll(columns.keySet());
        Map<String,Object> columnUpdates = columnNames.stream().collect(toMap(
                columnName -> columnName,
                columnName -> columns.get(columnName),
                (v1, v2) -> v1,
                LinkedHashMap::new // use a LinkedHashMap for consistent ordering gaurantees
        ));
        onDuplicateUpdateColumns = Optional.of(columnUpdates);
        return this;
    }

    /**
     * Set whether or not to insert null values by default. The default value for this is <code>true</code>.
     * @param excludeNullValues true to exclude inserting null values by default, false otherwise
     * @returng
     */
    public InsertBuilder excludeNullValues(boolean excludeNullValues) {
        this.excludeNullValues = excludeNullValues;
        return this;
    }

    public PreparedStatement build(Connection conn, int autoGeneratedKeys) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(generateSQL(), autoGeneratedKeys);
        int index = 1;
        for (String column : columns.keySet()) {
            ps.setObject(index++, columns.get(column));
        }
        if (onDuplicateUpdateColumns.isPresent()) {
            Map<String,Object> updateColumns = onDuplicateUpdateColumns.get();
            for (String column : updateColumns.keySet()) {
                ps.setObject(index++, updateColumns.get(column));
            }
        }
        return ps;
    }

    @VisibleForTesting
    protected String generateSQL() {
        StringBuilder sb = new StringBuilder("INSERT INTO " + table + " (");
        sb.append(StringUtils.join(columns.keySet(), ","));
        sb.append(") VALUES (");
        final String[] array = new String[columns.size()];
        Arrays.fill(array, "?");
        sb.append(StringUtils.join(asList(array), ","));
        sb.append(")");
        if (onDuplicateUpdateColumns.isPresent()) {
            Map<String,Object> updateColumns = onDuplicateUpdateColumns.get();
            sb.append(" ON DUPLICATE KEY UPDATE ");
            sb.append(updateColumns.keySet().stream().map(columnName -> columnName+"=?").collect(joining(",")));
        }
        return sb.toString();
    }

    private boolean shouldInsertValue(Object value, boolean shouldExcludeNullValues) {
        if (value != null) { // always insert non-null values
            return true;
        } else { // only insert the null value if we are not excluding null values
            return !shouldExcludeNullValues;
        }
    }
}
