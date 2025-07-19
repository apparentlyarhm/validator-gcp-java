package com.arhum.validator.controller;

import com.arhum.validator.model.response.CommonResponse;
import com.arhum.validator.model.response.LoginResponse;
import com.arhum.validator.service.contract.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@CrossOrigin
@RestController
@RequestMapping(value = "api/v2/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth Controller", description = "Auth Related APIs")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/login")
    @Operation(description = "Initiate the login via GitHub, this just generates the link using clientID")
    public CommonResponse login() {
        return authService.getGitHubLoginUrl();
    }

    @GetMapping("/callback")
    @Operation(description = "Callback URI. Exchanges code for JWT, then redirects user to the frontend.")
    public ResponseEntity<Void> callback(@RequestParam String code) {
        LoginResponse loginResponse = authService.issueJwtToken(code);
        String token = loginResponse.getToken();

        // This will create a URL like: http://localhost:3000/login-success?token=ey...
        URI redirectUri = UriComponentsBuilder
                .fromUriString("http://localhost:3000")
                .path("/login-success")
                .queryParam("token", token)
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }
}
