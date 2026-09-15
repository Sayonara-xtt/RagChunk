package com.xtsh.ragchunk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Isolates persistence tests from the application's pending Jackson 2/3 migration. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresPersistenceTestConfig {

    @Bean
    ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper();
    }
}
