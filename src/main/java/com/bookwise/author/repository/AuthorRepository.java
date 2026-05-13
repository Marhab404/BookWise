package com.bookwise.author.repository;

import com.bookwise.author.entity.Author;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findAllByOrderByFullNameAsc();
}
