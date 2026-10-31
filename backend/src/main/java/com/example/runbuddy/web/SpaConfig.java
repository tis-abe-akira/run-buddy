package com.example.runbuddy.web;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built frontend (packaged into classpath:/static by the "app" Maven profile) from the
 * same origin as the API.
 *
 * <p>The frontend routes in the browser, so a URL like /plans/2026-11 has no file behind it: such
 * paths get index.html. API paths and missing files (anything with an extension) still answer 404,
 * so a wrong API call never comes back as an HTML page. Controllers take precedence over this
 * handler, so existing endpoints are untouched. Without a built frontend nothing changes.
 */
@Configuration(proxyBeanMethods = false)
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    static final class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String path, Resource location) throws IOException {
            Resource requested = location.createRelative(path);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (isServerPath(path) || isFile(path)) {
                return null;
            }
            Resource index = location.createRelative("index.html");
            return index.exists() && index.isReadable() ? index : null;
        }

        private static boolean isServerPath(String path) {
            return path.startsWith("api/") || path.equals("api") || path.startsWith("v3/") || path.equals("health");
        }

        private static boolean isFile(String path) {
            return path.substring(path.lastIndexOf('/') + 1).contains(".");
        }
    }
}
