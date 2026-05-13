package com.bookwise.book.service;

import com.bookwise.author.entity.Author;
import com.bookwise.author.repository.AuthorRepository;
import com.bookwise.book.dto.BookFormBase;
import com.bookwise.book.dto.CreateBookForm;
import com.bookwise.book.dto.UpdateBookForm;
import com.bookwise.book.entity.Book;
import com.bookwise.book.entity.BookFile;
import com.bookwise.book.entity.BookFileType;
import com.bookwise.book.repository.BookFileRepository;
import com.bookwise.book.repository.BookRepository;
import com.bookwise.category.entity.Category;
import com.bookwise.category.repository.CategoryRepository;
import com.bookwise.common.MoneyUtils;
import com.bookwise.storage.FileStorageService;
import com.bookwise.storage.StoredFile;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookFileRepository bookFileRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final MoneyUtils moneyUtils;

    public BookService(
            BookRepository bookRepository,
            BookFileRepository bookFileRepository,
            AuthorRepository authorRepository,
            CategoryRepository categoryRepository,
            FileStorageService fileStorageService,
            MoneyUtils moneyUtils
    ) {
        this.bookRepository = bookRepository;
        this.bookFileRepository = bookFileRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
        this.moneyUtils = moneyUtils;
    }

    public List<Book> listPublishedBooks() {
        return bookRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    public List<Book> listAllBooks() {
        return bookRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Book> listFeaturedBooks() {
        List<Book> books = listPublishedBooks();
        return books.size() <= 6 ? books : books.subList(0, 6);
    }

    public Book getPublicBook(Long id) {
        return bookRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    public Book getAdminBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    public List<Book> getPurchasedBooks(Long userId) {
        return bookRepository.findPurchasedBooksByUserId(userId);
    }

    public List<BookFile> getBookFiles(Long bookId) {
        return bookFileRepository.findByBookIdOrderByCreatedAtAsc(bookId);
    }

    @Transactional
    public Book create(CreateBookForm form) {
        Book book = new Book();
        map(book, form);
        book = bookRepository.save(book);
        applyUploads(book, form);
        return bookRepository.save(book);
    }

    @Transactional
    public Book update(Long id, UpdateBookForm form) {
        Book book = getAdminBook(id);
        String oldCover = book.getCoverImagePath();
        map(book, form);
        applyUploads(book, form);
        if (form.getCoverImage() != null && !form.getCoverImage().isEmpty() && StringUtils.hasText(oldCover)) {
            fileStorageService.deleteIfExists(oldCover);
        }
        return bookRepository.save(book);
    }

    @Transactional
    public Book publish(Long id, boolean published) {
        Book book = getAdminBook(id);
        book.setPublished(published);
        return bookRepository.save(book);
    }

    private void map(Book book, BookFormBase form) {
        book.setTitle(form.getTitle().trim());
        book.setSubtitle(form.getSubtitle());
        book.setDescription(form.getDescription());
        book.setIsbn(StringUtils.hasText(form.getIsbn()) ? form.getIsbn().trim() : null);
        book.setPublicationYear(form.getPublicationYear());
        book.setLanguage(form.getLanguage().trim());
        book.setPriceAmount(moneyUtils.toMinorUnits(form.getPriceAmount(), currency(form.getCurrency())));
        book.setCurrency(currency(form.getCurrency()));
        book.setPublished(Boolean.TRUE.equals(form.getPublished()));
        book.setAuthors(loadAuthors(form.getAuthorIds()));
        book.setCategories(loadCategories(form.getCategoryIds()));
    }

    private void applyUploads(Book book, BookFormBase form) {
        if (form.getCoverImage() != null && !form.getCoverImage().isEmpty()) {
            StoredFile storedCover = fileStorageService.storeBookCover(book.getId(), form.getCoverImage());
            book.setCoverImagePath(storedCover.storageKey());
        }

        List<MultipartFile> files = normalizeFiles(form.getDigitalFiles());
        for (MultipartFile file : files) {
            StoredFile storedFile = fileStorageService.storeBookFile(book.getId(), file);
            BookFile bookFile = new BookFile();
            bookFile.setBook(book);
            bookFile.setFileName(storedFile.originalFileName());
            bookFile.setStorageKey(storedFile.storageKey());
            bookFile.setFileType(detectBookFileType(storedFile.originalFileName()));
            bookFile.setFileSize(storedFile.size());
            book.getFiles().add(bookFile);
        }
    }

    private List<MultipartFile> normalizeFiles(MultipartFile[] files) {
        if (files == null) {
            return List.of();
        }
        List<MultipartFile> valid = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                valid.add(file);
            }
        }
        return valid;
    }

    private Set<Author> loadAuthors(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Long> uniqueIds = ids.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Author> authors = authorRepository.findAllById(uniqueIds);
        if (authors.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("One or more authors were not found");
        }
        Map<Long, Author> byId = authors.stream().collect(Collectors.toMap(Author::getId, Function.identity()));
        return uniqueIds.stream().map(byId::get).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Category> loadCategories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<Long> uniqueIds = ids.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Category> categories = categoryRepository.findAllById(uniqueIds);
        if (categories.size() != uniqueIds.size()) {
            throw new IllegalArgumentException("One or more categories were not found");
        }
        Map<Long, Category> byId = categories.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
        return uniqueIds.stream().map(byId::get).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private BookFileType detectBookFileType(String fileName) {
        String extension = extension(fileName);
        if ("pdf".equalsIgnoreCase(extension)) {
            return BookFileType.PDF;
        }
        if ("epub".equalsIgnoreCase(extension)) {
            return BookFileType.EPUB;
        }
        throw new IllegalArgumentException("Book files must be PDF or EPUB");
    }

    private String extension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        return index == -1 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String currency(String currency) {
        return StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : "MAD";
    }
}
