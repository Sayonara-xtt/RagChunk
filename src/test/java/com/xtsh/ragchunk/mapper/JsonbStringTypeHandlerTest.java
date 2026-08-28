package com.xtsh.ragchunk.mapper;

import com.xtsh.ragchunk.mapper.typehandler.JsonbStringTypeHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
class JsonbStringTypeHandlerTest {

    private final JsonbStringTypeHandler handler = new JsonbStringTypeHandler();

    @Test
    void bindsJsonAsPostgresJsonb() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        String json = "{\"strategy\":\"hybrid\"}";

        handler.setNonNullParameter(statement, 2, json, null);

        var expected = new PGobject();
        expected.setType("jsonb");
        expected.setValue(json);
        verify(statement).setObject(2, expected);
    }

    @Test
    void readsJsonAsStringAndPreservesNull() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("config_json")).thenReturn("{\"enabled\":true}");

        assertEquals("{\"enabled\":true}", handler.getNullableResult(resultSet, "config_json"));
        assertNull(handler.getNullableResult(mock(ResultSet.class), "config_json"));
    }
}
