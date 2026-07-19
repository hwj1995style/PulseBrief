package com.pulsebrief.admin.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AdminIdentityService {
    public static final String PRINCIPAL_ATTRIBUTE = AdminIdentityService.class.getName() + ".principal";

    public AdminPrincipal current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            Object value = request.getAttribute(PRINCIPAL_ATTRIBUTE);
            if (value instanceof AdminPrincipal principal) {
                return principal;
            }
        }
        return new AdminPrincipal(null, "system", "System", "SYSTEM", false);
    }
}
