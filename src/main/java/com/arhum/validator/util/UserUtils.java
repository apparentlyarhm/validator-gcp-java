package com.arhum.validator.util;

import com.arhum.validator.model.LoggedInUser;
import com.arhum.validator.model.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserUtils {

    public LoggedInUser getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Boolean isAdmin = authentication
                .getAuthorities()
                .stream()
                .anyMatch(grantedAuthority -> grantedAuthority
                        .getAuthority()
                        .equals(Role.ROLE_ADMIN.name())
                );

        LoggedInUser user = new LoggedInUser();
        user.setUsername(authentication.getName());
        user.setIsAdmin(isAdmin);

        return user;
    }
}
