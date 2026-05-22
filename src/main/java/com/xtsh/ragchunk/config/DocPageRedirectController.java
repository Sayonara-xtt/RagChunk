package com.xtsh.ragchunk.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 兼容旧书签：/doc.html 重定向到 Scalar API 文档（Swagger UI 仍可通过 /swagger-ui/index.html 访问）。
 */
@Controller
public class DocPageRedirectController {

    @GetMapping("/doc.html")
    public String docHtml() {
        return "redirect:/scalar";
    }
}
