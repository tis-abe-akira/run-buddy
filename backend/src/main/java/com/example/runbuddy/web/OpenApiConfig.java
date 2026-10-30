package com.example.runbuddy.web;

import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    /**
     * Marks every property of every schema as required.
     *
     * <p>Our request and response bodies are records whose fields are always serialized, so a
     * property is never absent; at most it is {@code null}, which is declared per field with
     * {@code @Schema(types = {..., "null"})}. Without this the generated TypeScript types would make
     * every field optional.
     */
    @Bean
    OpenApiCustomizer allPropertiesRequired() {
        return openApi -> openApi.getComponents().getSchemas().values().forEach(OpenApiConfig::requireAll);
    }

    private static void requireAll(Schema<?> schema) {
        if (schema.getProperties() != null) {
            schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        }
    }
}
