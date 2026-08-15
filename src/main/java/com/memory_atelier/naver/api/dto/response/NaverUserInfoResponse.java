package com.memory_atelier.naver.api.dto.response;

public record NaverUserInfoResponse(
        String resultcode,
        String message,
        Response response
) {
    public record Response(
            String id,
            String email,
            String nickname
    ){}

    public String getEmail() {
        return response.email();
    }

    public String getNickname() {
        return response.nickname();
    }
}