package com.bookwise.author.service;

import com.bookwise.author.dto.AuthorForm;
import com.bookwise.author.entity.Author;
import com.bookwise.author.repository.AuthorRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<Author> listAll() {
        return authorRepository.findAllByOrderByFullNameAsc();
    }

    public Author getById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Author not found"));
    }

    @Transactional
    public Author create(AuthorForm form) {
        Author author = new Author();
        map(author, form);
        return authorRepository.save(author);
    }

    @Transactional
    public Author update(Long id, AuthorForm form) {
        Author author = getById(id);
        map(author, form);
        return authorRepository.save(author);
    }

    @Transactional
    public void delete(Long id) {
        Author author = getById(id);
        if (authorRepository.existsLinkedToBooks(id)) {
            throw new IllegalArgumentException("Author cannot be deleted while it is still assigned to books.");
        }
        authorRepository.delete(author);
    }

    private void map(Author author, AuthorForm form) {
        author.setFullName(form.getFullName().trim());
        author.setBiography(form.getBiography());
    }
}
