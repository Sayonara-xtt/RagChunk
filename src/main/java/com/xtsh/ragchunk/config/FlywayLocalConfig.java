package com.xtsh.ragchunk.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 本机 {@code local} 环境：启动前执行 Flyway repair，避免已应用的 V2 脚本被修改后 checksum 不一致导致无法启动。
 * <p>
 * repair 只更新 {@code flyway_schema_history} 中的校验和，不会重复执行迁移 SQL。
 */
@Configuration
@Profile("local")
public class FlywayLocalConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
