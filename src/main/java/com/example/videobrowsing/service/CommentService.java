package com.example.videobrowsing.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.videobrowsing.dto.CommentDTO;
import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.entity.CommentLike;
import com.example.videobrowsing.entity.User;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.repository.CommentLikeRepository;
import com.example.videobrowsing.repository.CommentRepository;
import com.example.videobrowsing.repository.UserRepository;
import com.example.videobrowsing.repository.VideoRepository;

@Service
public class CommentService {
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    public Comment addComment(Comment comment) {
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByVideoId(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return commentRepository.findByVideo(video);
    }

    @Transactional
    public Comment updateComment(Long commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        comment.setContent(newContent.trim());
        comment.setUpdatedAt(LocalDateTime.now());
        
        return commentRepository.save(comment);
    }

    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public CommentDTO createComment(Long videoId, Long userId, String content, Long parentCommentId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Comment comment = new Comment();
        comment.setVideo(video);
        comment.setUser(user);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        if (parentCommentId != null) {
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!parent.getVideo().getId().equals(videoId)) {
                throw new IllegalArgumentException("Parent comment does not belong to this video");
            }
            comment.setParentComment(parent);
        }

        Comment saved = commentRepository.save(comment);
        return toDto(saved, false, user, Collections.singletonMap(saved.getId(), 0L), Collections.emptySet(), Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentDtosForVideo(Long videoId, User currentUser) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        List<Comment> roots = commentRepository.findVideoCommentsOrderedByPinnedAndDate(video).stream()
                .filter(this::isActiveComment)
                .collect(Collectors.toList());

        if (roots.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<Comment>> replyMap = new HashMap<>();
        Set<Long> commentIds = new HashSet<>();
        roots.forEach(root -> populateReplyTree(root, replyMap, commentIds));

        Map<Long, Long> likeCounts = commentIds.isEmpty()
                ? Collections.emptyMap()
                : toLikeCountMap(commentLikeRepository.countLikesByCommentIds(commentIds));

        Set<Long> likedByCurrentUser = (currentUser != null && !commentIds.isEmpty())
                ? new HashSet<>(commentLikeRepository.findCommentIdsLikedByUser(commentIds, currentUser))
                : Collections.emptySet();

        return roots.stream()
                .map(comment -> toDto(comment, true, currentUser, likeCounts, likedByCurrentUser, replyMap))
                .collect(Collectors.toList());
    }

    public void deleteComment(Long videoId, Long commentId, User requester) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getVideo().getId().equals(videoId)) {
            throw new IllegalArgumentException("Comment does not belong to specified video");
        }

        boolean isOwner = comment.getUser() != null && comment.getUser().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new IllegalStateException("Not authorized to delete this comment");
        }

        commentRepository.delete(comment);
    }

    private boolean isActiveComment(Comment comment) {
        return !Boolean.TRUE.equals(comment.getIsDisabled());
    }

    @Transactional
    public Map<String, Object> toggleCommentLike(Long videoId, Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (comment.getVideo() == null || !comment.getVideo().getId().equals(videoId)) {
            throw new IllegalArgumentException("Comment does not belong to specified video");
        }

        boolean liked;
        CommentLike existing = commentLikeRepository.findByCommentAndUser(comment, user).orElse(null);
        if (existing != null) {
            commentLikeRepository.delete(existing);
            liked = false;
        } else {
            CommentLike like = new CommentLike();
            like.setComment(comment);
            like.setUser(user);
            like.setCreatedAt(LocalDateTime.now());
            commentLikeRepository.save(like);
            liked = true;
        }

        long likeCount = commentLikeRepository.countByComment(comment);
        Map<String, Object> payload = new HashMap<>();
        payload.put("commentId", commentId);
        payload.put("liked", liked);
        payload.put("likeCount", likeCount);
        return payload;
    }

    private CommentDTO toDto(Comment comment, boolean includeReplies, User currentUser,
                              Map<Long, Long> likeCounts,
                              Set<Long> likedCommentIds,
                              Map<Long, List<Comment>> replyMap) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setVideoId(comment.getVideo() != null ? comment.getVideo().getId() : null);
        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId());
            dto.setUsername(comment.getUser().getUsername());
            dto.setUserProfilePicture(comment.getUser().getProfilePicture());
        }
        dto.setContent(comment.getContent());
        dto.setIsPinned(comment.getPinned());
        dto.setIsSpam(comment.getSpam());
        dto.setIsDisabled(comment.getIsDisabled());
        dto.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null);
        dto.setCreatedAt(comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null);
        dto.setLikeCount(likeCounts.getOrDefault(comment.getId(), 0L));
        dto.setLikedByCurrentUser(currentUser != null && likedCommentIds.contains(comment.getId()));

        if (includeReplies) {
            List<Comment> replies = replyMap.getOrDefault(comment.getId(), Collections.emptyList());
            List<CommentDTO> replyDtos = replies.stream()
                    .map(reply -> toDto(reply, true, currentUser, likeCounts, likedCommentIds, replyMap))
                    .collect(Collectors.toList());
            dto.setReplies(replyDtos);
        } else {
            dto.setReplies(Collections.emptyList());
        }
        return dto;
    }

    private void populateReplyTree(Comment comment,
                                    Map<Long, List<Comment>> replyMap,
                                    Set<Long> commentIds) {
        if (comment == null || !isActiveComment(comment)) {
            return;
        }

        commentIds.add(comment.getId());
        List<Comment> replies = commentRepository.findByParentComment(comment).stream()
                .filter(this::isActiveComment)
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .collect(Collectors.toCollection(ArrayList::new));
        replyMap.put(comment.getId(), replies);
        replies.forEach(reply -> populateReplyTree(reply, replyMap, commentIds));
    }

    private Map<Long, Long> toLikeCountMap(List<Object[]> aggregates) {
        if (aggregates == null || aggregates.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> likeCounts = new HashMap<>();
        for (Object[] row : aggregates) {
            if (row == null || row.length < 2) {
                continue;
            }
            Long commentId = (Long) row[0];
            Long count = row[1] instanceof Long ? (Long) row[1] : ((Number) row[1]).longValue();
            likeCounts.put(commentId, count);
        }
        return likeCounts;
    }
}
