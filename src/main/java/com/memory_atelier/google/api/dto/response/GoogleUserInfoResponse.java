package com.memory_atelier.google.api.dto.response;

public record GoogleUserInfoResponse(
        String sub,
        String email,
        String name,
        String picture
) {
    public String getNickname() {
        if (name == null) {
            return "구글사용자";
        }
        return name;
    }

    public String getEmail() {
        if (email == null) {
            return "test@google.com";
        }
        return email;
    }
}
