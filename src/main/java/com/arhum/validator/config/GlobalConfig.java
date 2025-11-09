package com.arhum.validator.config;

import com.arhum.validator.model.enums.Role;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "auth.security")
public class GlobalConfig {
    private static final Logger logger = LoggerFactory.getLogger(GlobalConfig.class);

    private List<String> adminEmails;
    private List<String> userEmails;

    private Set<String> admins;
    private Set<String> users;

    @PostConstruct
    public void init() {
        this.admins = parse(adminEmails);
        this.users = parse(userEmails);

        logger.info("Configured {} app admins and {} authorised users for a subset of RCON commands", admins.size(), users.size());
    }

    private Set<String> parse(List<String> s) {
        return Arrays
                .stream(s.get(0)
                        .split(" "))
                .collect(Collectors.toSet()
                );
    }

    public Role getRole(String email) {
       return admins.contains(email) ? Role.ROLE_ADMIN :
               users.contains(email) ? Role.ROLE_USER : Role.ROLE_ANON;
    }
}
