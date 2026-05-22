package com.xtsh.ragchunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 需本地 Docker 可用时运行：{@code set RUN_PG_INTEGRATION=1 && mvn test -Dtest=RagChunkPostgresIntegrationTest}
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_PG_INTEGRATION", matches = "1")
class RagChunkPostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("ragchunk")
            .withUsername("ragchunk")
            .withPassword("ragchunk");

    @Test
    void contextLoadsWithPostgres() {
    }
}
