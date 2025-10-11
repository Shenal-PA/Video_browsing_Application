package com.example.videobrowsing.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileStorageConfig implements WebMvcConfigurer {

	private static final Logger log = LoggerFactory.getLogger(FileStorageConfig.class);

	@Value("${file.upload-dir}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
		Path uploadsRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
		ensureDirectories(uploadsRoot);

		String uploadsLocation = uploadsRoot.toUri().toString();
		if (!uploadsLocation.endsWith("/")) {
			uploadsLocation = uploadsLocation + "/";
		}

		registry.addResourceHandler("/uploads/**")
				.addResourceLocations(uploadsLocation)
				.setCacheControl(CacheControl.noCache())
				.resourceChain(true);
	}

	private void ensureDirectories(Path uploadsRoot) {
		try {
			Files.createDirectories(uploadsRoot);
			Files.createDirectories(uploadsRoot.resolve("videos"));
			Files.createDirectories(uploadsRoot.resolve("thumbnails"));
		} catch (IOException ex) {
			log.error("Failed to create upload directories at {}", uploadsRoot, ex);
		}
	}
}
