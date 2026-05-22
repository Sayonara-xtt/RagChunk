package com.xtsh.ragchunk.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ragChunkOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RagChunk API")
                        .description("一期 RAG 知识库：创建库 → 上传文档 → 混合切片 → 向量检索问答")
                        .version("v1")
                        .contact(new Contact().name("RagChunk"))
                        .license(new License().name("Apache 2.0")));
    }
}
