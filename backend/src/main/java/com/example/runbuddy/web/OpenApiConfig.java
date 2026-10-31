package com.example.runbuddy.web;

import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
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
     *
     * <p>A nullable reference to another schema is rewritten as {@code anyOf: [$ref, null]}, the
     * form TypeScript generators understand.
     */
    @Bean
    OpenApiCustomizer allPropertiesRequired() {
        return openApi -> openApi.getComponents().getSchemas().values().forEach(OpenApiConfig::requireAll);
    }

    private static void requireAll(Schema<?> schema) {
        if (schema.getProperties() == null) {
            return;
        }
        schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        schema.getProperties().replaceAll((name, property) -> nullableReference(property));
    }

    private static Schema<?> nullableReference(Schema<?> property) {
        boolean nullable = property.getTypes() != null && property.getTypes().contains("null");
        if (property.get$ref() == null || !nullable) {
            return property;
        }
        Schema<Object> nullSchema = new Schema<>();
        nullSchema.addType("null");
        return new Schema<>().anyOf(List.of(new Schema<>().$ref(property.get$ref()), nullSchema));
    }
}
