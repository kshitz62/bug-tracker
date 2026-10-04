package com.bugtracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves the static HTML/CSS/JS frontend located in the workspace `frontend/` directory.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontendDir = Paths.get("frontend").toAbsolutePath().normalize();
        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + frontendDir.toString().replace('\\', '/') + "/")
                .setCachePeriod(0);
    }
}
