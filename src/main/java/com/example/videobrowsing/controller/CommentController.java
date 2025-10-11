package com.example.videobrowsing.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.service.CommentService;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    @Autowired
    private CommentService commentService;

    // Add a new comment
    @PostMapping
    public ResponseEntity<Comment> addComment(@RequestBody Comment comment) {
        return ResponseEntity.ok(commentService.addComment(comment));
    }

    // Get all comments for a video
    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<Comment>> getCommentsByVideo(@PathVariable Long videoId) {
        return ResponseEntity.ok(commentService.getCommentsByVideoId(videoId));
    }

    // Update/Edit a comment
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long commentId, 
            @RequestBody Map<String, String> payload) {
        try {
            System.out.println("=== EDIT COMMENT REQUEST ===");
            System.out.println("Comment ID: " + commentId);
            System.out.println("Payload: " + payload);
            
            String content = payload.get("content");
            if (content == null || content.trim().isEmpty()) {
                System.out.println("ERROR: Content is null or empty");
                return ResponseEntity.badRequest().body("Comment content cannot be empty");
            }
            
            System.out.println("Content to update: " + content);
            Comment updated = commentService.updateComment(commentId, content);
            System.out.println("Successfully updated comment: " + updated.getId());
            
            // Return a simple response instead of the full entity to avoid serialization issues
            Map<String, Object> response = new HashMap<>();
            response.put("id", updated.getId());
            response.put("content", updated.getContent());
            response.put("createdAt", updated.getCreatedAt());
            response.put("updatedAt", updated.getUpdatedAt());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            System.out.println("ERROR: Unexpected exception");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating comment: " + e.getMessage());
        }
    }

    // Delete a comment
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }

    
}