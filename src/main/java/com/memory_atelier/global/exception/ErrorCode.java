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
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "502", "외부 API 연동 중 오류가 발생했습니다."),

    // 도메인별 에러 코드는 각 도메인 작업 시 여기에 추가

    //User 도메인
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "404-1", "존재하지 않는 사용자입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "409-1", "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "400-1", "이메일 또는 비밀번호가 일치하지 않습니다."),

    // Auth 도메인
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "401-1", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "401-2", "저장된 리프레시 토큰을 찾을 수 없습니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "401-3", "만료된 리프레시 토큰입니다."),

    // 카카오 로그인
    KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "401-4", "카카오 로그인에 실패했습니다."),
    KAKAO_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "400-2", "카카오 계정에서 이메일을 가져올 수 없습니다."),

    // 구글 로그인
    GOOGLE_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "401-5", "구글 로그인에 실패했습니다."),
    GOOGLE_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "400-3", "구글 계정에서 이메일을 가져올 수 없습니다."),

    // 네이버 로그인
    NAVER_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "401-6", "네이버 로그인에 실패했습니다."),
    NAVER_EMAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "400-4", "네이버 계정에서 이메일을 가져올 수 없습니다."),

    // S3
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S3_500", "파일 업로드에 실패했습니다."),

    // Memory 도메인
    MEMORY_NOT_FOUND(HttpStatus.NOT_FOUND, "M_404", "존재하지 않는 추억입니다."),
    INVALID_ITEM_OPTION(HttpStatus.BAD_REQUEST, "M_400", "선택할 수 없는 항목입니다."),
    REFINED_STORY_NOT_READY(HttpStatus.CONFLICT, "M_409", "AI로 다듬은 사연이 아직 없습니다. 먼저 분석을 실행해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    // 스프링이 자체적으로 처리한 예외의 상태 코드를 대응되는 ErrorCode로 되돌린다
    // 도메인 코드가 늘어나도 매핑이 흔들리지 않도록 선언 순서에 의존하지 않고 명시적으로 나열한다
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
