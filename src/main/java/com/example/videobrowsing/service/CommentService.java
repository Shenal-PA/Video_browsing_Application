package com.example.videobrowsing.service;

import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.CommentRepository;
import com.example.videobrowsing.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private VideoRepository videoRepository;

    public Comment addComment(Comment comment) {
        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsByVideoId(Long videoId) {
        Optional<Video> videoOpt = videoRepository.findById(videoId);
        return videoOpt.map(commentRepository::findByVideo).orElse(List.of());
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
