package com.xtsh.ragchunk;

import com.xtsh.ragchunk.config.RagChunkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
@SpringBootApplication
@EnableConfigurationProperties(RagChunkProperties.class)
public class RagChunkApplication {

    public static void main(String[] args) {
        var app = new SpringApplication(RagChunkApplication.class);
        // IDEA 未加载 yaml 或 classpath 缺依赖时的兜底（与 application.yaml local 一致）
        app.setDefaultProperties(java.util.Map.of(
                "spring.datasource.driver-class-name", "org.postgresql.Driver",
                "spring.datasource.url", "jdbc:postgresql://localhost:5432/ragchunk",
                "spring.datasource.username", "postgres",
                "spring.datasource.password", "123"
        ));
        app.run(args);
    }
}
