package com.example.videobrowsing.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.videobrowsing.dto.VideoDTO;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.service.UserService;
import com.example.videobrowsing.service.VideoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private VideoService videoService;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/index")
    public String index() {
        return "index";
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/profile/{userId}")
    public String viewUserProfile(@PathVariable Long userId, Model model) {
        model.addAttribute("viewUserId", userId);
        return "profile";
    }

    @GetMapping("/creator/{creatorId}")
    public String viewCreatorProfile(@PathVariable Long creatorId, Model model) {
        model.addAttribute("creatorId", creatorId);
        return "profile";
    }

    @GetMapping("/video-upload")
    public String videoUpload() {
        return "video-upload";
    }

    @GetMapping("/video-edit")
    public String videoEdit(@RequestParam(name = "id", required = false) Long videoId, Model model) {
        if (videoId != null) {
            model.addAttribute("videoId", videoId);
        }
        return "video-edit";
    }

    @GetMapping({"/video-show", "/video-show.html"})
    public String videoShow(@RequestParam(name = "id", required = false) Long videoId,
                            Model model,
                            HttpSession session) {
        model.addAttribute("requestedVideoId", videoId);
        model.addAttribute("initialVideo", null);
        model.addAttribute("initialVideoJson", null);

        if (videoId != null) {
            Optional<User> currentUser = resolveSessionUser(session);
            Optional<Video> videoOpt = videoService.getVideoById(videoId);
            videoOpt.ifPresent(video -> {
                VideoDTO dto = videoService.toDto(video, currentUser);
                model.addAttribute("initialVideo", dto);
                try {
                    model.addAttribute("initialVideoJson", objectMapper.writeValueAsString(dto));
                } catch (JsonProcessingException e) {
                    model.addAttribute("initialVideoJson", null);
                }
            });
        }

        return "video-show";
    }

    @GetMapping("/video-detail")
    public String legacyVideoDetail(@RequestParam(name = "id", required = false) Long videoId) {
        if (videoId != null) {
            return "redirect:/video-show?id=" + videoId;
        }
        return "redirect:/video-show";
    }

    @GetMapping("/playlist")
    public String playlist() {
        return "playlist";
    }

    @GetMapping("/admin-dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }

    @GetMapping("/trending")
    public String trending() {
        return "trending";
    }

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    @GetMapping("/help-center")
    public String helpCenter() {
        return "help-center";
    }

    @GetMapping("/help-center.html")
    public String helpCenterHtml() {
        return "help-center";
    }

    @GetMapping("/report-issue")
    public String reportIssue() {
        return "report-issue";
    }

    @GetMapping("/report-issue.html")
    public String reportIssueHtml() {
        return "report-issue";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }

    @GetMapping("/privacy.html")
    public String privacyHtml() {
        return "privacy";
    }

    @GetMapping("/all-videos")
    public String allVideos() {
        return "all-videos";
    }

    @GetMapping({"/become-creator", "/become-creator.html"})
    public String becomeCreator() {
        return "become-creator";
    }

    @GetMapping("/my-videos")
    public String myVideos() {
        return "profile"; // My videos are shown on the profile page
    }

    private Optional<User> resolveSessionUser(HttpSession session) {
        return userService.resolveCurrentUser(session);
    }
}
