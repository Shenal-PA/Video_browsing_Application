package com.example.videobrowsing;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.dto.CommentDTO;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.UserRepository;
import com.example.videobrowsing.repository.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registeredUserCanPostAndRetrieveComments() throws Exception {
        User user = ensureTestUser();
        Video video = ensureTestVideo(user);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole().name());

        String commentContent = "Loved the pacing of this video!";

        MvcResult postResult = mockMvc.perform(post("/api/videos/{videoId}/comments", video.getId())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentPayload(commentContent))))
                .andExpect(status().isOk())
                .andReturn();

        CommentDTO created = objectMapper.readValue(postResult.getResponse().getContentAsString(), CommentDTO.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getVideoId()).isEqualTo(video.getId());
        assertThat(created.getUserId()).isEqualTo(user.getId());
        assertThat(created.getContent()).isEqualTo(commentContent);

        MvcResult getResult = mockMvc.perform(get("/api/videos/{videoId}/comments", video.getId())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String payload = getResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(payload).contains(commentContent);
    }

    @Test
    void securityContextUserWithoutSessionAttributesCanPostComments() throws Exception {
        User user = ensureTestUser();
        Video video = ensureTestVideo(user);

        String commentContent = "Security context syncs session";

        MvcResult postResult = mockMvc.perform(post("/api/videos/{videoId}/comments", video.getId())
                        .with(user(user.getUsername()).roles("REGISTERED_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentPayload(commentContent))))
                .andExpect(status().isOk())
                .andReturn();

        CommentDTO created = objectMapper.readValue(postResult.getResponse().getContentAsString(), CommentDTO.class);
        assertThat(created.getContent()).isEqualTo(commentContent);

        mockMvc.perform(get("/api/videos/{videoId}/comments", video.getId())
                        .with(user(user.getUsername()).roles("REGISTERED_USER")))
                .andExpect(status().isOk());
    }

    private User ensureTestUser() {
        return userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User user = new User();
            user.setUsername("testuser-" + UUID.randomUUID());
            user.setEmail("test-" + UUID.randomUUID() + "@example.com");
            user.setPassword("plain-password");
            user.setFirstname("Test");
            user.setLastname("User");
            user.setProfilePicture("https://example.com/avatar.png");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    private Video ensureTestVideo(User uploader) {
        return videoRepository.findAll().stream().findFirst().orElseGet(() -> {
            Video video = new Video();
            video.setTitle("Integration Test Video");
            video.setDescription("Video used for integration testing of comments");
            video.setFilepath("/uploads/videos/test-video.mp4");
            video.setUploadedBy(uploader);
            video.setStatus(Video.Status.PUBLISHED);
            video.setPrivacy(Video.Privacy.PUBLIC);
            video.setCreatedAt(LocalDateTime.now());
            video.setUpdatedAt(LocalDateTime.now());
            return videoRepository.save(video);
        });
    }

    private record CommentPayload(String content) { }
}
