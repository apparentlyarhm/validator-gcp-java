package com.arhum.validator.config.auth;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.exception.InvalidTokenException;
import com.arhum.validator.model.enums.Role;
import com.arhum.validator.service.impl.ValidatorServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class GlobalRequestFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(GlobalRequestFilter.class);

    @Value("${github.authorized-email}")
    private String authorizedEmail;

    @Autowired
    private WebClient webClient;

    // So GitHub directly doesn't give JWTs so it's just a hacky way to fetch currently logged-in user's email, and that's really what we want.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (Objects.nonNull(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("TOKEN FOUND :: {}", token);

            try {
                Map<String, Object> userInfo = webClient.get()
                        .uri("https://api.github.com/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                String email = (String) userInfo.get("email");
                logger.info("email :: {}", email);

                if (email != null) {
                    List<GrantedAuthority> authorities = Objects.equals(email, authorizedEmail)
                            ? List.of(new SimpleGrantedAuthority(String.valueOf(Role.ADMIN)), new SimpleGrantedAuthority(String.valueOf(Role.USER)))
                            : List.of(new SimpleGrantedAuthority(String.valueOf(Role.USER)));

                    logger.info("CURRENT LOGGED IN USER HAS :: {} roles", authorities.size()); // 2 means an admin

                    Authentication auth = new UsernamePasswordAuthenticationToken(email, null, authorities); // we don't maintain any credentials
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            } catch (Exception e) {
                // if the token in invalid, webclient itself will throw exception because the http request will have non-200 status code
                logger.error("GOT EXCEPTION FROM REQUEST :: {} ", e.getMessage());
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid GitHub token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
