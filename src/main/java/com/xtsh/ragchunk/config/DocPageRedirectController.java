package com.xtsh.ragchunk.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Knife4j 4.5 与 SpringDoc 3 不兼容时，将 /doc.html 重定向到可用的 Swagger UI。
 */
@Controller
public class DocPageRedirectController {

    @GetMapping("/doc.html")
    public String docHtml() {
        return "redirect:/swagger-ui/index.html";
    }
}
