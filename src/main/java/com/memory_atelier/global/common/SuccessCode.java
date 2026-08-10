package com.memory_atelier.global.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessCode {

    OK(HttpStatus.OK, "200", "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, "201", "리소스가 생성되었습니다.");

    // 도메인별 성공 코드는 각 도메인 작업 시 여기에 추가

    private final HttpStatus status;
    private final String code;
    private final String message;

    SuccessCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
