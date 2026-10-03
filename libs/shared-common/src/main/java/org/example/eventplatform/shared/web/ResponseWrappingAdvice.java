package org.example.eventplatform.shared.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

/**
 * Wraps every controller response in ApiResponse without touching a single
 * controller — nothing here is written by hand except inside
 * GlobalExceptionHandler, which builds ApiResponse.error(...) directly (this
 * advice recognizes that and passes it through unwrapped).
 */
@RestControllerAdvice
public class ResponseWrappingAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ResponseWrappingAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        // Internal S2S payloads stay unwrapped so RestClient callers can bind
        // straight to DTOs / arrays without peeling ApiResponse.
        String path = request.getURI().getPath();
        if (path != null && path.startsWith("/api/internal")) {
            return body;
        }
        if (body instanceof ApiResponse<?>) {
            return body;
        }
        // Nội dung nhị phân (ảnh, file) phải trả nguyên, bọc vào JSON sẽ làm hỏng file.
        if (body instanceof byte[] || body instanceof org.springframework.core.io.Resource) {
            return body;
        }
        // A null body (e.g. ResponseEntity.noContent().build()) means the method
        // deliberately wants no body written — a 204 must stay bodyless.
        if (body == null) {
            return null;
        }
        Object envelope = ApiResponse.success(body);
        // The converter was already picked from the endpoint's declared return
        // type (e.g. a controller returning ResponseEntity<String>) before this
        // advice runs. StringHttpMessageConverter can only write a String, so
        // handing it the ApiResponse object throws a ClassCastException — write
        // the JSON ourselves instead.
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return objectMapper.writeValueAsString(envelope);
        }
        return envelope;
    }
}
