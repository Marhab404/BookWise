package com.bookwise.category.repository;

import com.bookwise.category.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            select case when count(b) > 0 then true else false end
            from Category c
            join c.books b
            where c.id = :categoryId
            """)
    boolean existsLinkedToBooks(@Param("categoryId") Long categoryId);
}
