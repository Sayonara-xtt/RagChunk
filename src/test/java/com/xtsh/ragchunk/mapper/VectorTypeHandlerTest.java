package com.xtsh.ragchunk.mapper;

import com.pgvector.PGvector;
import com.xtsh.ragchunk.mapper.typehandler.PgVectorTypeHandler;
import com.xtsh.ragchunk.mapper.typehandler.RealArrayTypeHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class VectorTypeHandlerTest {

    @Test
    void bindsFloatArrayAsPgVector() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);

        new PgVectorTypeHandler().setNonNullParameter(statement, 1, new float[]{1.0f, 2.5f}, null);

        ArgumentCaptor<PGvector> vector = ArgumentCaptor.forClass(PGvector.class);
        verify(statement).setObject(eq(1), vector.capture());
        assertArrayEquals(new float[]{1.0f, 2.5f}, vector.getValue().toArray());
    }

    @Test
    void bindsAndReadsPostgresRealArray() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        Connection connection = mock(Connection.class);
        Array sqlArray = mock(Array.class);
        when(statement.getConnection()).thenReturn(connection);
        when(connection.createArrayOf(eq("real"), any(Float[].class))).thenReturn(sqlArray);

        var handler = new RealArrayTypeHandler();
        handler.setNonNullParameter(statement, 3, new float[]{0.25f, 0.75f}, null);

        verify(connection).createArrayOf("real", new Float[]{0.25f, 0.75f});
        verify(statement).setArray(3, sqlArray);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getArray("embedding")).thenReturn(sqlArray);
        when(sqlArray.getArray()).thenReturn(new Float[]{0.25f, null, 0.75f});
        assertArrayEquals(new float[]{0.25f, 0.0f, 0.75f},
                handler.getNullableResult(resultSet, "embedding"));
    }
}
