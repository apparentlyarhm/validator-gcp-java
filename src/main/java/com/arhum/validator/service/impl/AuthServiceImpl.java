package com.arhum.validator.service.impl;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.model.enums.Role;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.GithubTokenResponse;
import com.arhum.validator.model.response.LoginResponse;
import com.arhum.validator.service.contract.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class AuthServiceImpl  implements AuthService {

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${console.host}")
    private String frontendHost;

    @Value("${github.authorized-email}")
    private String authorizedEmail;

    @Value("${auth.security.signing-secret}")
    private String jwtSecret;

    @Autowired
    private WebClient webClient;

    @Override
    public CommonResponse getGitHubLoginUrl() {
        String red = frontendHost + "/callback";
        String url = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri",red )
                .queryParam("scope", "read:user user:email")
                .build()
                .toUriString();
        return new CommonResponse(url);
    }

    @Override
    public LoginResponse issueJwtToken(String code) throws BaseException {
        GithubTokenResponse githubToken = exchangeCodeForToken(code);

        // Github response is a big JSON, no point creating a DTO for it...
        Map<String, Object> userInfo = webClient.get()
                .uri("https://api.github.com/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken.getAccessToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        String email = (String) userInfo.get("email");
        String id = (String) userInfo.get("login");
        String role = authorizedEmail.equals(email) ? String.valueOf(Role.ROLE_ADMIN) : String.valueOf(Role.ROLE_USER);

        String jwt = Jwts.builder()
                .setSubject("github|" + id)
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        LoginResponse response = new LoginResponse();
        response.setId(id);
        response.setEmail(email);
        response.setToken(jwt);

        return response;
    }

    private GithubTokenResponse exchangeCodeForToken(String code) {
        String red = frontendHost + "/callback";
        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", red
        );

        return webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new InternalServerException("GitHub Token Error: " + error, 500))))
                .bodyToMono(GithubTokenResponse.class)
                .block();
    }
}
