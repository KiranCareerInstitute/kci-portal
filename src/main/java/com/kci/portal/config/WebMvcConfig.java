package com.kci.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.path.doubts}")
    private String doubtUploadPath;

    @Value("${upload.path.tests}")
    private String testsDir;
    @Value("${upload.path.assignment}")
    private String assignmentUploadPath;

    @Value("${upload.path.videos}")
    private String videosUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/doubts/**")
                .addResourceLocations(Paths.get(doubtUploadPath).toUri().toString());

        registry.addResourceHandler("/uploads/assignment/**")
                .addResourceLocations(Paths.get(assignmentUploadPath).toUri().toString());

        registry.addResourceHandler("/uploads/videos/**")
                .addResourceLocations("file:" + videosUploadPath + "/");

        registry.addResourceHandler("/uploads/test_submissions/**")
                .addResourceLocations("file:" + testsDir + "/");


    }
}
