package com.kci.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class VideoResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String videoUploadPath = Paths.get("uploads/videos").toAbsolutePath().toUri().toString();
        if (!videoUploadPath.endsWith("/")) {
            videoUploadPath += "/";
        }

        registry.addResourceHandler("/uploads/videos/**")
                .addResourceLocations(videoUploadPath);
    }
}
