package com.xtsh.ragchunk;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("inmemory")
@Tag("integration")
class RagChunkApplicationTests {

    @Test
    void contextLoads() {
    }
}
