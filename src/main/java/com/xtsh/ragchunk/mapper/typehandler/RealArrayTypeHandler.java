package com.xtsh.ragchunk.mapper.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** PostgreSQL {@code real[]} binding for domain-level {@code float[]} values. */
public class RealArrayTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, float[] parameter, JdbcType jdbcType)
            throws SQLException {
        Float[] boxed = new Float[parameter.length];
        for (int i = 0; i < parameter.length; i++) {
            boxed[i] = parameter[i];
        }
        statement.setArray(index, statement.getConnection().createArrayOf("real", boxed));
    }

    @Override
    public float[] getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return toFloatArray(resultSet.getArray(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return toFloatArray(resultSet.getArray(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return toFloatArray(statement.getArray(columnIndex));
    }

    private static float[] toFloatArray(java.sql.Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        Object values = sqlArray.getArray();
        int length = Array.getLength(values);
        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            Object value = Array.get(values, i);
            result[i] = value instanceof Number number ? number.floatValue() : 0.0f;
        }
        return result;
    }
}
