package com.bookwise.book.controller;

import com.bookwise.book.entity.Book;
import com.bookwise.book.service.BookService;
import com.bookwise.storage.FileStorageService;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/covers")
public class BookCoverController {

    private final BookService bookService;
    private final FileStorageService fileStorageService;

    public BookCoverController(BookService bookService, FileStorageService fileStorageService) {
        this.bookService = bookService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<Resource> cover(@PathVariable Long bookId) {
        try {
            Book book = bookService.getPublicBook(bookId);
            if (book.getCoverImagePath() != null && !book.getCoverImagePath().isBlank()) {
                Resource resource = fileStorageService.loadAsResource(book.getCoverImagePath());
                MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache())
                        .contentType(mediaType)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                        .body(resource);
            }
        } catch (IllegalArgumentException ignored) {
        }
        Resource fallback = new ClassPathResource("static/images/default-cover.svg");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(fallback);
    }
}
