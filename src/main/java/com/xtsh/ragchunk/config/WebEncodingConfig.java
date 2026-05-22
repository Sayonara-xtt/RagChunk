package com.xtsh.ragchunk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 强制 JSON / 文本响应使用 UTF-8，避免中文乱码。
 */
@Configuration
public class WebEncodingConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        var utf8 = StandardCharsets.UTF_8;
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jackson) {
                jackson.setDefaultCharset(utf8);
            }
            if (converter instanceof StringHttpMessageConverter string) {
                string.setDefaultCharset(utf8);
                string.setWriteAcceptCharset(false);
            }
        }
    }
}
