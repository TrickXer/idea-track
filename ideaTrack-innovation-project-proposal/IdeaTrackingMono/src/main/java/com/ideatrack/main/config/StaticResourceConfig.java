package com.ideatrack.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Absolute path to your uploads directory
        String uploadAbsPath = Paths.get("uploads/profile-pics/").toAbsolutePath().toString();

        registry.addResourceHandler("/uploads/profile-pics/**")
                .addResourceLocations("file:" + uploadAbsPath + "/")
                .setCachePeriod(60 * 60 * 24 * 30); // 30 days cache
    }
}