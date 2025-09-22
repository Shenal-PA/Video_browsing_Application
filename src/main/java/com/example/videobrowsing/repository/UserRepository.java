package com.example.videobrowsing.repository;

import com.example.videobrowsing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);
    List<User> findByIsActiveTrue();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") User.Role role);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.firstName LIKE %:keyword% OR u.lastName LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);
}

