package com.example.videobrowsing;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.util.UriUtils;

import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.VideoRepository;

@SpringBootTest
@AutoConfigureMockMvc
class VideoStreamingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VideoRepository videoRepository;

    @Test
    void videoShowPageContainsVideoSource() throws Exception {
        Video sampleVideo = videoRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No videos found in database"));

        MvcResult result = mockMvc.perform(get("/video-show").param("id", sampleVideo.getId().toString()))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        String expectedVideoUrl = buildEncodedVideoUrl(sampleVideo);
        assertThat(html)
                .as("video-show page should contain the encoded video URL")
                .contains(expectedVideoUrl);

    assertThat(html)
        .as("video-show page should expose the video id via data attribute")
        .contains("data-video-id=\"" + sampleVideo.getId() + "\"");
    }

    @Test
    void uploadedVideoResourceIsReachable() throws Exception {
        Video sampleVideo = videoRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No videos found in database"));

        String expectedVideoUrl = buildEncodedVideoUrl(sampleVideo);

    MvcResult result = mockMvc.perform(get(URI.create(expectedVideoUrl))
            .with(user("videoTester").roles("REGISTERED_USER")))
        .andExpect(status().isOk())
        .andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    private String buildEncodedVideoUrl(Video video) {
        String rawPath = video.getFilepath();
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalStateException("Video filepath is missing");
        }

    String normalized = rawPath.trim().replace("\\", "/");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }

        String resolved;
        if (normalized.startsWith("/")) {
            resolved = normalized;
        } else if (normalized.startsWith("uploads/") || normalized.startsWith("videos/")
                || normalized.startsWith("video/") || normalized.startsWith("static/")) {
            resolved = "/" + normalized;
        } else if (normalized.contains("/")) {
            resolved = normalized.startsWith("/") ? normalized : "/" + normalized;
        } else {
            resolved = "/uploads/videos/" + normalized;
        }

        return encodeRelativePath(resolved);
    }

    private String encodeRelativePath(String path) {
        boolean leadingSlash = path.startsWith("/");
        String working = leadingSlash ? path.substring(1) : path;

        String encoded = Arrays.stream(working.split("/"))
                .filter(segment -> segment != null && !segment.isBlank())
                .map(this::decodeThenEncode)
                .collect(Collectors.joining("/"));

        return leadingSlash ? "/" + encoded : encoded;
    }

    private String decodeThenEncode(String segment) {
        String current = segment;
        for (int i = 0; i < 3; i++) {
            try {
                String decoded = URLDecoder.decode(current, StandardCharsets.UTF_8);
                if (decoded.equals(current)) {
                    break;
                }
                current = decoded;
            } catch (IllegalArgumentException ex) {
                break;
            }
        }
        return UriUtils.encodePathSegment(current, StandardCharsets.UTF_8);
    }
}
