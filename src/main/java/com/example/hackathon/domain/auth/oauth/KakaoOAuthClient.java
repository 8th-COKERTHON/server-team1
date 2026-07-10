package com.example.hackathon.domain.auth.oauth;

import com.example.hackathon.domain.user.entity.Provider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    @Value("${oauth.kakao.client-id}")
    private String clientId;
    @Value("${oauth.kakao.client-secret}")
    private String clientSecret;
    @Value("${oauth.kakao.redirect-uri}")
    private String defaultRedirectUri;

    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        String accessToken = requestAccessToken(code, redirectUri);
        KakaoUserResponse user = restClient.get()
                .uri(USER_INFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class);

        String email = (user.kakaoAccount() != null) ? user.kakaoAccount().email() : null;
        String nickname = (user.kakaoAccount() != null && user.kakaoAccount().profile() != null)
                ? user.kakaoAccount().profile().nickname()
                : "카카오유저";

        return new OAuthUserInfo(Provider.KAKAO, String.valueOf(user.id()), email, nickname);
    }

    private String requestAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri);
        form.add("code", code);

        KakaoTokenResponse token = restClient.post()
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        return token.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUserResponse(
            Long id,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record KakaoAccount(String email, Profile profile) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            private record Profile(String nickname) {
            }
        }
    }
}
