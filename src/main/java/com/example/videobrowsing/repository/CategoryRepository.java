package com.example.videobrowsing.repository;

import com.example.videobrowsing.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    Boolean existsByName(String name);

    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword% OR c.description LIKE %:keyword%")
    List<Category> searchCategories(@Param("keyword") String keyword);

    @Query("SELECT c FROM Category c ORDER BY c.name ASC")
    List<Category> findAllOrderByName();


}
