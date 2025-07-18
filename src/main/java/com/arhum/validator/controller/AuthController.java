package com.arhum.validator.controller;

import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.GithubTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping(value = "api/v2/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth Controller", description = "Auth Related APIs")
public class AuthController {
    // TODO: move these to service.

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @Autowired
    private WebClient webClient;

    @GetMapping("/login")
    @Operation(description = "Initiate the login via GitHub")
    public CommonResponse login() {
        String url = UriComponentsBuilder
                .fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user%20user:email")
                .build()
                .toUriString();

        return new CommonResponse(url);
    }

    @GetMapping("/callback")
    @Operation(description = "Callback URI that exchanges the temp code for access tokens")
    public GithubTokenResponse callback(@RequestParam String code) {

        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("redirect_uri", redirectUri); // This is optional but recommended


        return webClient
                .post()
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
