package com.memory_atelier.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.memory_atelier.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return success(SuccessCode.OK, data);
    }

    public static ResponseEntity<ApiResponse<Void>> success() {
        return successEmpty(SuccessCode.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(SuccessCode successCode, T data) {
        return ResponseEntity.status(successCode.getStatus())
                .body(new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), data));
    }

    public static ResponseEntity<ApiResponse<Void>> successEmpty(SuccessCode successCode) {
        return ResponseEntity.status(successCode.getStatus())
                .body(new ApiResponse<>(true, successCode.getCode(), successCode.getMessage(), null));
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(errorBody(errorCode, message));
    }

    public static ApiResponse<Void> errorBody(ErrorCode errorCode) {
        return errorBody(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> errorBody(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }
}
