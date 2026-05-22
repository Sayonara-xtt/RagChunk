package com.xtsh.ragchunk.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.xtsh.ragchunk.persistence.mapper")
@ConditionalOnProperty(name = "ragchunk.storage.mode", havingValue = "postgres", matchIfMissing = true)
public class MybatisConfig {
}
