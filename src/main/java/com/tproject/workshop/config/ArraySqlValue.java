package com.tproject.workshop.config;

import com.tproject.workshop.exception.BadRequestException;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.jdbc.support.SqlValue;

import java.sql.Array;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Allow to spring jdbc to convert array of any primitive type, plus String, to preparedStatements
 */
public final class ArraySqlValue implements SqlValue {
    private final Object[] arr;
    private final String dbTypeName;

    private ArraySqlValue(final Object[] arr, final String dbTypeName) {
        if (arr == null || dbTypeName == null) {
            throw new BadRequestException();
        }
        this.arr = arr.clone();
        this.dbTypeName = dbTypeName;
    }

    public static ArraySqlValue create(final Object[] arr) {
        return new ArraySqlValue(arr, determineDbTypeName(arr));
    }

    public static ArraySqlValue create(final Object[] arr, final String dbTypeName) {
        return new ArraySqlValue(arr, dbTypeName);
    }

    private static String determineDbTypeName(final Object[] arr) {
        final int sqlParameterType =
                StatementCreatorUtils.javaTypeToSqlParameterType(arr.getClass().getComponentType());
        final JDBCType jdbcTypeToUse = JDBCType.valueOf(sqlParameterType);
        return jdbcTypeToUse.getName().toLowerCase(Locale.US);
    }

    @Override
    public void setValue(final PreparedStatement ps, final int paramIndex) throws SQLException {
        final Array arrayValue = ps.getConnection().createArrayOf(dbTypeName, arr);
        ps.setArray(paramIndex, arrayValue);
    }

    @Override
    public void cleanup() {
        // Doing nothing because its not used yet
    }
}
