package com.xtsh.ragchunk.web;

import com.xtsh.ragchunk.web.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ApiResponse(responseCode = "400", description = "请求参数错误",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> badRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("INVALID_ARGUMENT", ex.getMessage()));
    }

    @ApiResponse(responseCode = "404", description = "资源不存在",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ApiResponse(responseCode = "500", description = "服务内部错误",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> internal(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", ex.getMessage() != null ? ex.getMessage() : "error"));
    }
}
