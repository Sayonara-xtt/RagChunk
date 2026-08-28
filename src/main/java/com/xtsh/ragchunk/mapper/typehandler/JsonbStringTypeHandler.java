package com.xtsh.ragchunk.mapper.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Keeps PostgreSQL JSONB binding at the persistence boundary while the domain
 * mapping continues to use a serialized JSON string.
 */
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, String parameter, JdbcType jdbcType)
            throws SQLException {
        var jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(parameter);
        statement.setObject(index, jsonb);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return resultSet.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return statement.getString(columnIndex);
    }
}
