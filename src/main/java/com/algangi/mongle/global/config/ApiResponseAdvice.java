package com.algangi.mongle.global.config;

import com.algangi.mongle.global.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * REST API 응답을 자동으로 ApiResponse로 래핑하는 Advice
 *
 * 이미 ApiResponse인 경우나 에러 응답인 경우는 그대로 반환합니다.
 */
@RestControllerAdvice(basePackages = "com.algangi.mongle")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // ApiResponse가 아닌 경우에만 적용
        return !returnType.getParameterType().equals(ApiResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 이미 ApiResponse인 경우 그대로 반환
        if (body instanceof ApiResponse) {
            return body;
        }

        // Void 또는 null인 경우 데이터 없이 성공 응답
        if (body == null || returnType.getParameterType().equals(Void.TYPE)) {
            return ApiResponse.success();
        }

        // 일반 데이터는 ApiResponse로 래핑하여 반환
        return ApiResponse.success(body);
    }
}
