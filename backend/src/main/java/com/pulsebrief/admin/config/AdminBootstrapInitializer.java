package com.pulsebrief.admin.config;

import com.pulsebrief.admin.service.AdminAuthService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapInitializer implements ApplicationRunner {
    private final AdminAuthService authService;

    public AdminBootstrapInitializer(AdminAuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.bootstrapIfConfigured();
    }
}
