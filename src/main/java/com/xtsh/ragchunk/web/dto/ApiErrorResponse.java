package com.xtsh.ragchunk.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一错误响应")
public record ApiErrorResponse(
        @Schema(description = "错误码", example = "NOT_FOUND", allowableValues = {"INVALID_ARGUMENT", "NOT_FOUND", "INTERNAL_ERROR"})
        String code,
        @Schema(description = "错误说明", example = "knowledge base not found: kb_xxx")
        String message
) {}
