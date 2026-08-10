package com.memory_atelier.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {

    // 4xx
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "404", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "허용되지 않은 HTTP 메서드입니다."),
    CONFLICT(HttpStatus.CONFLICT, "409", "요청이 현재 리소스 상태와 충돌합니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "429", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),

    // 5xx
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "502", "외부 API 연동 중 오류가 발생했습니다.");

    // 도메인별 에러 코드는 각 도메인 작업 시 여기에 추가

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    /**
     * 스프링이 자체적으로 처리한 예외의 상태 코드를 대응되는 ErrorCode로 되돌린다.
     * 도메인 코드가 늘어나도 매핑이 흔들리지 않도록 선언 순서에 의존하지 않고 명시적으로 나열한다.
     */
    public static ErrorCode from(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> INVALID_INPUT;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 429 -> TOO_MANY_REQUESTS;
            case 502 -> EXTERNAL_API_ERROR;
            default -> statusCode.is4xxClientError() ? INVALID_INPUT : INTERNAL_SERVER_ERROR;
        };
    }
}
