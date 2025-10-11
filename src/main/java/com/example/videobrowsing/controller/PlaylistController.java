package com.example.videobrowsing.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.videobrowsing.dto.PlaylistDTO;
import com.example.videobrowsing.entity.Playlist;
import com.example.videobrowsing.entity.PlaylistVideo;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.service.PlaylistService;
import com.example.videobrowsing.service.UserService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "*")
public class PlaylistController {

	@Autowired
	private PlaylistService playlistService;

	@Autowired
	private UserService userService;

	@GetMapping("/my")
	public ResponseEntity<?> getMyPlaylists(HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		if (currentUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
		}

		List<PlaylistDTO> playlists = playlistService.getPlaylistSummaries(currentUser.get());
		return ResponseEntity.ok(playlists);
	}

	@GetMapping("/{playlistId}")
	public ResponseEntity<?> getPlaylist(@PathVariable Long playlistId, HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		try {
			PlaylistDTO playlist = playlistService.getPlaylistDetails(playlistId, currentUser.orElse(null));
			return ResponseEntity.ok(playlist);
		} catch (RuntimeException ex) {
			return buildErrorResponse(ex);
		}
	}

	@PostMapping
	public ResponseEntity<?> createPlaylist(@RequestBody PlaylistDTO playlistDTO, HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		if (currentUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
		}

		if (playlistDTO.getName() == null || playlistDTO.getName().isBlank()) {
			return ResponseEntity.badRequest().body("Playlist name is required");
		}

		Playlist playlist = playlistService.createPlaylist(playlistDTO, currentUser.get());
		PlaylistDTO response = playlistService.getPlaylistDetails(playlist.getId(), currentUser.get());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{playlistId}")
	public ResponseEntity<?> updatePlaylist(@PathVariable Long playlistId,
											@RequestBody PlaylistDTO playlistDTO,
											HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		if (currentUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
		}

		try {
			playlistService.updatePlaylist(playlistId, playlistDTO, currentUser.get());
			PlaylistDTO updated = playlistService.getPlaylistDetails(playlistId, currentUser.get());
			return ResponseEntity.ok(updated);
		} catch (RuntimeException ex) {
			return buildErrorResponse(ex);
		}
	}

	@DeleteMapping("/{playlistId}")
	public ResponseEntity<?> deletePlaylist(@PathVariable Long playlistId, HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		if (currentUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
		}

		try {
			playlistService.deletePlaylist(playlistId, currentUser.get());
			return ResponseEntity.noContent().build();
		} catch (RuntimeException ex) {
			return buildErrorResponse(ex);
		}
	}

	@PostMapping("/{playlistId}/videos")
	public ResponseEntity<?> addVideoToPlaylist(@PathVariable Long playlistId,
			@RequestBody(required = false) Map<String, Long> payload,
			HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		if (currentUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
		}

		Long videoId = payload != null ? payload.get("videoId") : null;
		if (videoId == null) {
			return ResponseEntity.badRequest().body("Video ID is required");
		}

		try {
			PlaylistVideo added = playlistService.addVideoToPlaylist(playlistId, videoId, currentUser.get());
			return ResponseEntity.ok(Map.of(
				"playlistId", added.getPlaylist().getId(),
				"videoId", added.getVideo().getId(),
				"position", added.getPosition()
			));
		} catch (RuntimeException ex) {
			return buildErrorResponse(ex);
		}
	}

	@DeleteMapping("/{playlistId}/videos/{videoId}")
	public ResponseEntity<?> removeVideoFromPlaylist(@PathVariable Long playlistId,
			@PathVariable Long videoId,
			HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		if (currentUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
		}

		try {
			playlistService.removeVideoFromPlaylist(playlistId, videoId, currentUser.get());
			PlaylistDTO updated = playlistService.getPlaylistDetails(playlistId, currentUser.get());
			return ResponseEntity.ok(updated);
		} catch (RuntimeException ex) {
			return buildErrorResponse(ex);
		}
	}

	@PutMapping("/{playlistId}/videos/reorder")
	public ResponseEntity<?> reorderPlaylistVideos(@PathVariable Long playlistId,
			@RequestBody(required = false) Map<String, List<Long>> payload,
			HttpSession session) {
		Optional<User> currentUser = resolveSessionUser(session);
		if (currentUser.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not logged in");
		}

		List<Long> videoIds = payload != null ? payload.get("videoIds") : null;
		if (videoIds == null || videoIds.isEmpty()) {
			return ResponseEntity.badRequest().body("Video order cannot be empty");
		}

		try {
			PlaylistDTO updated = playlistService.reorderPlaylistVideos(playlistId, videoIds, currentUser.get());
			return ResponseEntity.ok(updated);
		} catch (RuntimeException ex) {
			return buildErrorResponse(ex);
		}
	}

	private Optional<User> resolveSessionUser(HttpSession session) {
		Long userId = (Long) session.getAttribute("userId");
		if (userId == null) {
			return Optional.empty();
		}
		return userService.findById(userId);
	}

	private ResponseEntity<String> buildErrorResponse(RuntimeException ex) {
		String message = ex.getMessage() != null ? ex.getMessage() : "Request failed";
		if (message.toLowerCase().contains("not logged in")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message);
		}
		if (message.toLowerCase().contains("not found")) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
		}
		if (message.toLowerCase().contains("not authorized")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message);
		}
		return ResponseEntity.badRequest().body(message);
	}


	
}
