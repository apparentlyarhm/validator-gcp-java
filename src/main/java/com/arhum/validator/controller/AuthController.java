package com.arhum.validator.controller;

import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.GithubTokenResponse;
import com.arhum.validator.model.response.LoginResponse;
import com.arhum.validator.service.contract.AuthService;
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

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    @Operation(description = "Initiate the login via GitHub")
    public CommonResponse login() {
        return authService.getGitHubLoginUrl();
    }

    @GetMapping("/callback")
    @Operation(description = "Callback URI that exchanges the temp code for access tokens and generates server-issued JWT")
    public LoginResponse callback(@RequestParam String code) {
        return authService.issueJwtToken(code);
    }
}
