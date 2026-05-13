package com.bookwise.library.controller;

import com.bookwise.book.entity.BookFile;
import com.bookwise.library.service.LibraryService;
import com.bookwise.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my-library")
public class LibraryController {

    private final LibraryService libraryService;
    private final com.bookwise.storage.FileStorageService fileStorageService;

    public LibraryController(LibraryService libraryService, com.bookwise.storage.FileStorageService fileStorageService) {
        this.libraryService = libraryService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String library(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("books", libraryService.purchasedBooks(principal.id()));
        return "library/list";
    }

    @GetMapping("/books/{bookId}/read")
    public ResponseEntity<Resource> read(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long bookId) throws IOException {
        BookFile bookFile = libraryService.firstOwnedBookFile(principal.id(), bookId);
        Resource resource = fileStorageService.loadAsResource(bookFile.getStorageKey());
        MediaType mediaType = mediaTypeFor(bookFile);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + bookFile.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/books/{bookId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long bookId) throws IOException {
        BookFile bookFile = libraryService.firstOwnedBookFile(principal.id(), bookId);
        Resource resource = fileStorageService.loadAsResource(bookFile.getStorageKey());
        MediaType mediaType = mediaTypeFor(bookFile);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + bookFile.getFileName() + "\"")
                .body(resource);
    }

    private MediaType mediaTypeFor(BookFile bookFile) {
        return switch (bookFile.getFileType()) {
            case PDF -> MediaType.APPLICATION_PDF;
            case EPUB -> MediaType.parseMediaType("application/epub+zip");
        };
    }
}
