package com.arhum.validator.config.auth;

import com.arhum.validator.exception.BaseException;
import com.arhum.validator.exception.InternalServerException;
import com.arhum.validator.exception.InvalidTokenException;
import com.arhum.validator.model.enums.Role;
import com.arhum.validator.service.impl.ValidatorServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import java.nio.charset.StandardCharsets;
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

    @Value("${auth.security.signing-secret}")
    private String jwtSecret;

    // So GitHub directly doesn't give JWTs so made our own
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        logger.info("headers :: {}", request.getHeaderNames());
        if (Objects.nonNull(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("TOKEN FOUND :: {}", token);

            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                logger.info("Extracted user: {}, role: {}", email, role);

                if (email != null && role != null) {
                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role.toUpperCase()));
                    Authentication auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }


            } catch (JwtException e) {
                logger.error("JWT validation failed: {}", e.getMessage());
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid GitHub token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
