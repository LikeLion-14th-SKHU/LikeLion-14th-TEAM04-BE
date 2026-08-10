package com.memory_atelier.global.security;

import com.memory_atelier.global.common.ApiResponse;
import com.memory_atelier.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class SecurityErrorResponder {

    private SecurityErrorResponder() {
    }

    static void respond(HttpServletResponse response, JsonMapper jsonMapper, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getWriter(), ApiResponse.errorBody(errorCode));
    }
}
