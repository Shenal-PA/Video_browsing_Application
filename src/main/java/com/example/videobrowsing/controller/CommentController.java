package com.example.videobrowsing.controller;


import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import java.util.List;

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

    // Delete a comment
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }
}