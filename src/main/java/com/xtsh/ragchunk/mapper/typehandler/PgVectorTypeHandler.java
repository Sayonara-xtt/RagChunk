package com.xtsh.ragchunk.mapper.typehandler;

import com.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** PostgreSQL pgvector binding for domain-level {@code float[]} values. */
public class PgVectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, float[] parameter, JdbcType jdbcType)
            throws SQLException {
        statement.setObject(index, new PGvector(parameter));
    }

    @Override
    public float[] getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return toFloatArray(resultSet.getObject(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return toFloatArray(resultSet.getObject(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return toFloatArray(statement.getObject(columnIndex));
    }

    private static float[] toFloatArray(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof PGvector vector) {
            return vector.toArray();
        }
        return new PGvector(value.toString()).toArray();
    }
}
