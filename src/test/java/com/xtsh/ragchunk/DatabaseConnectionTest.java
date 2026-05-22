package com.xtsh.ragchunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/** 由 {@code scripts/test-db-connection.ps1} 触发，设置 {@code RUN_DB_TEST=1} */
@EnabledIfEnvironmentVariable(named = "RUN_DB_TEST", matches = "1")
@SpringBootTest
@ActiveProfiles("local")
class DatabaseConnectionTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void connectionAndSchema() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            assertTrue(conn.isValid(5));
        }

        String db = jdbc.queryForObject("SELECT current_database()", String.class);
        String user = jdbc.queryForObject("SELECT current_user", String.class);
        Boolean vectorExt = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')", Boolean.class);
        Integer kbCount = jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_base", Integer.class);

        System.out.println("database=" + db + ", user=" + user + ", pgvector=" + vectorExt + ", knowledge_base rows=" + kbCount);
        assertNotNull(db);
        assertTrue(vectorExt, "pgvector extension should be installed");
    }
}
