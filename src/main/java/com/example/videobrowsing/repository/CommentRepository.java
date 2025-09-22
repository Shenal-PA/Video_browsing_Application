package com.example.videobrowsing.repository;

import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.entity.Video;
import com.example.videobrowsing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByVideo(Video video);
    List<Comment> findByUser(User user);
    List<Comment> findByVideoAndParentCommentIsNull(Video video);
    List<Comment> findByParentComment(Comment parentComment);

    @Query("SELECT c FROM Comment c WHERE c.video = :video AND c.isDisabled = false AND c.parentComment IS NULL ORDER BY c.isPinned DESC, c.createdAt DESC")
    List<Comment> findVideoCommentsOrderedByPinnedAndDate(@Param("video") Video video);

    @Query("SELECT c FROM Comment c WHERE c.isSpam = true AND c.isDisabled = false")
    List<Comment> findSpamComments();

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.video = :video AND c.isDisabled = false")
    Long countByVideoAndNotDisabled(@Param("video") Video video);
}
