package com.memory_atelier.kakao.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(

        Long id,

        @JsonProperty("properties")
        Properties properties,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {

    public record Properties(
            String nickname
    ){
    }

    public record KakaoAccount(
            String email
    ){
    }

    public String getNickname(){
        if (properties == null || properties.nickname() == null){
            return "카카오사용자";
        }

        return properties.nickname();
    }

    public String getEmail() {
        if (kakaoAccount == null || kakaoAccount.email() == null){
            return "test@kakao.com";
        }
        return kakaoAccount.email();
    }
}