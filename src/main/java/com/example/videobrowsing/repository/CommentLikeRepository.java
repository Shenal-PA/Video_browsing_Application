package com.example.videobrowsing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.videobrowsing.entity.Comment;
import com.example.videobrowsing.entity.CommentLike;
import com.example.videobrowsing.entity.User;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    long countByComment(Comment comment);

    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);

    @Query("SELECT cl.comment.id, COUNT(cl) FROM CommentLike cl WHERE cl.comment.id IN :commentIds GROUP BY cl.comment.id")
    List<Object[]> countLikesByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    @Query("SELECT cl.comment.id FROM CommentLike cl WHERE cl.comment.id IN :commentIds AND cl.user = :user")
    List<Long> findCommentIdsLikedByUser(@Param("commentIds") Collection<Long> commentIds, @Param("user") User user);
}
