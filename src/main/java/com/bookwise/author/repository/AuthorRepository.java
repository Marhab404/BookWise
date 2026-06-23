package com.bookwise.author.repository;

import com.bookwise.author.entity.Author;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findAllByOrderByFullNameAsc();

    @Query("""
            select case when count(b) > 0 then true else false end
            from Author a
            join a.books b
            where a.id = :authorId
            """)
    boolean existsLinkedToBooks(@Param("authorId") Long authorId);
}
