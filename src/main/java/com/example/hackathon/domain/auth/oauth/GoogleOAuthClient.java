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
public class GoogleOAuthClient {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URI = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestClient restClient = RestClient.create();

    @Value("${oauth.google.client-id}")
    private String clientId;
    @Value("${oauth.google.client-secret}")
    private String clientSecret;
    @Value("${oauth.google.redirect-uri}")
    private String defaultRedirectUri;

    public OAuthUserInfo getUserInfo(String code, String redirectUri) {
        String accessToken = requestAccessToken(code, redirectUri);
        GoogleUserResponse user = restClient.get()
                .uri(USER_INFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(GoogleUserResponse.class);

        String nickname = (user.name() != null) ? user.name() : "구글유저";
        return new OAuthUserInfo(Provider.GOOGLE, user.id(), user.email(), nickname);
    }

    private String requestAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : defaultRedirectUri);
        form.add("code", code);

        GoogleTokenResponse token = restClient.post()
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);

        return token.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleUserResponse(String id, String email, String name) {
    }
}
