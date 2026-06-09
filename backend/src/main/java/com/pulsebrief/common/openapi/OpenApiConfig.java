package com.pulsebrief.common.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PulseBrief V1 API",
                version = "v1.0.0",
                description = "PulseBrief mobile app backend API for global news brief, digest, subscription, and user history flows."
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development")
        },
        tags = {
                @Tag(name = "Health", description = "Service health check"),
                @Tag(name = "Auth", description = "Mock login and guest session"),
                @Tag(name = "Articles", description = "Home feed and article detail"),
                @Tag(name = "Categories", description = "News categories"),
                @Tag(name = "Digests", description = "Daily digest and audio briefing"),
                @Tag(name = "Subscriptions", description = "User subscription settings"),
                @Tag(name = "User", description = "User profile, favorites, and reading history"),
                @Tag(name = "Playback", description = "Audio playback history")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "dev-token"
)
public class OpenApiConfig {
}
