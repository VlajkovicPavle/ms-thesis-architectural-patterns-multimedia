package dev.pavle.mediamodular.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "app.storage.local")
public record StorageProperties(@NotBlank String path) {}
