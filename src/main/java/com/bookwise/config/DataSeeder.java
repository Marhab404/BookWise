package com.bookwise.config;

import com.bookwise.author.entity.Author;
import com.bookwise.author.repository.AuthorRepository;
import com.bookwise.book.entity.Book;
import com.bookwise.book.entity.BookFile;
import com.bookwise.book.entity.BookFileType;
import com.bookwise.book.repository.BookRepository;
import com.bookwise.category.entity.Category;
import com.bookwise.category.repository.CategoryRepository;
import com.bookwise.config.StorageProperties;
import com.bookwise.user.entity.User;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(value = "app.seed-demo", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageProperties storageProperties;

    public DataSeeder(
            AuthorRepository authorRepository,
            CategoryRepository categoryRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            StorageProperties storageProperties
    ) {
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageProperties = storageProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        seedAdminIfConfigured();
        seedCatalogIfEmpty();
    }

    private void seedAdminIfConfigured() {
        if (userRepository.existsByEmailIgnoreCase(adminEmail())) {
            return;
        }

        if (!StringUtils.hasText(System.getenv("BOOKWISE_ADMIN_PASSWORD"))) {
            log.warn("BOOKWISE_ADMIN_PASSWORD is not set; skipping admin seed");
            return;
        }

        User admin = new User();
        admin.setFullName(adminFullName());
        admin.setEmail(adminEmail().toLowerCase(Locale.ROOT));
        admin.setPasswordHash(passwordEncoder.encode(System.getenv("BOOKWISE_ADMIN_PASSWORD")));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
        log.info("Seeded admin user {}", admin.getEmail());
    }

    private void seedCatalogIfEmpty() throws IOException {
        List<Author> authors = authorRepository.findAll();
        if (authors.isEmpty()) {
            authors = authorRepository.saveAll(List.of(
                    author("Meriem El Idrissi", "A contemporary Moroccan novelist focused on digital culture and identity."),
                    author("Youssef Benali", "Writer of thoughtful fiction and essays about learning."),
                    author("Sara Lamrani", "Author of approachable non-fiction for modern readers.")
            ));
        }

        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            categories = categoryRepository.saveAll(List.of(
                    category("Fiction", "Narrative works for leisure and imagination."),
                    category("Business", "Practical reading for entrepreneurs and professionals."),
                    category("Technology", "Books for software and digital skills.")
            ));
        }

        if (bookRepository.count() > 0) {
            return;
        }

        Path storageRoot = Paths.get(storageProperties.root()).toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
        Files.createDirectories(storageRoot.resolve("covers/books"));
        Files.createDirectories(storageRoot.resolve("books"));

        Category fiction = categories.stream().filter(c -> c.getName().equalsIgnoreCase("Fiction")).findFirst().orElse(categories.getFirst());
        Category business = categories.stream().filter(c -> c.getName().equalsIgnoreCase("Business")).findFirst().orElse(categories.getFirst());
        Category technology = categories.stream().filter(c -> c.getName().equalsIgnoreCase("Technology")).findFirst().orElse(categories.getFirst());

        Author author1 = authors.getFirst();
        Author author2 = authors.size() > 1 ? authors.get(1) : authors.getFirst();
        Author author3 = authors.size() > 2 ? authors.get(2) : authors.getFirst();

        seedBook(storageRoot, "The Quiet Library", "Stories from a digital reading room", "A gentle fiction title for the demo catalog.", "9780000000011", 2024, "English", 2990L, fiction, author1, "the-quiet-library");
        seedBook(storageRoot, "Bank Transfer Basics", "A practical guide to manual payments", "A short business book about payment reconciliation and admin review.", "9780000000028", 2023, "English", 3990L, business, author2, "bank-transfer-basics");
        seedBook(storageRoot, "Modern Spring Boot", "Build server-rendered apps with confidence", "A technical book for readers exploring Spring Boot and secure file delivery.", "9780000000035", 2025, "English", 4990L, technology, author3, "modern-spring-boot");

        log.info("Seeded demo catalog with authors, categories, and sample books");
    }

    private Book seedBook(Path storageRoot,
                          String title,
                          String subtitle,
                          String description,
                          String isbn,
                          int year,
                          String language,
                          Long priceAmount,
                          Category category,
                          Author author,
                          String filePrefix) throws IOException {
        Book book = new Book();
        book.setTitle(title);
        book.setSubtitle(subtitle);
        book.setDescription(description);
        book.setIsbn(isbn);
        book.setPublicationYear(year);
        book.setLanguage(language);
        book.setPriceAmount(priceAmount);
        book.setCurrency("MAD");
        book.setPublished(true);
        book.getAuthors().add(author);
        book.getCategories().add(category);
        Book saved = bookRepository.save(book);

        String coverKey = writeDemoCover(storageRoot, saved.getId(), title);
        saved.setCoverImagePath(coverKey);

        String bookKey = writeDemoPdf(storageRoot, saved.getId(), filePrefix);
        BookFile file = new BookFile();
        file.setBook(saved);
        file.setFileName(title + ".pdf");
        file.setStorageKey(bookKey);
        file.setFileType(BookFileType.PDF);
        file.setFileSize(Files.size(resolveAbsolute(storageRoot, bookKey)));
        saved.getFiles().add(file);

        return bookRepository.save(saved);
    }

    private String writeDemoCover(Path storageRoot, Long bookId, String title) throws IOException {
        Path coverDir = storageRoot.resolve("covers/books/" + bookId);
        Files.createDirectories(coverDir);
        Path cover = coverDir.resolve("demo-cover.svg");
        String svg = """
                <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"600\" height=\"900\" viewBox=\"0 0 600 900\">
                  <rect width=\"600\" height=\"900\" rx=\"32\" fill=\"#0f766e\"/>
                  <rect x=\"36\" y=\"36\" width=\"528\" height=\"828\" rx=\"24\" fill=\"#ecfeff\" opacity=\"0.12\"/>
                  <text x=\"60\" y=\"170\" fill=\"#f8fafc\" font-family=\"Arial, sans-serif\" font-size=\"40\" font-weight=\"700\">BookWise</text>
                  <text x=\"60\" y=\"270\" fill=\"#f8fafc\" font-family=\"Arial, sans-serif\" font-size=\"44\" font-weight=\"700\">%s</text>
                  <text x=\"60\" y=\"340\" fill=\"#cffafe\" font-family=\"Arial, sans-serif\" font-size=\"22\">Digital Library Demo</text>
                </svg>
                """.formatted(escapeXml(title));
        Files.writeString(cover, svg, StandardCharsets.UTF_8);
        return storageRoot.relativize(cover).toString().replace('\\', '/');
    }

    private String writeDemoPdf(Path storageRoot, Long bookId, String filePrefix) throws IOException {
        Path bookDir = storageRoot.resolve("books/" + bookId);
        Files.createDirectories(bookDir);
        Path pdf = bookDir.resolve(filePrefix + ".pdf");
        String content = "%PDF-1.4\n" +
                "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n" +
                "2 0 obj<< /Type /Pages /Count 1 /Kids [3 0 R] >>endobj\n" +
                "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] /Contents 4 0 R /Resources <<>> >>endobj\n" +
                "4 0 obj<< /Length 44 >>stream\n" +
                "BT /F1 24 Tf 50 100 Td (BookWise demo file) Tj ET\n" +
                "endstream endobj\n" +
                "xref\n0 5\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000116 00000 n \n0000000221 00000 n \n" +
                "trailer<< /Root 1 0 R /Size 5 >>\nstartxref\n310\n%%EOF";
        Files.writeString(pdf, content, StandardCharsets.ISO_8859_1);
        return storageRoot.relativize(pdf).toString().replace('\\', '/');
    }

    private Path resolveAbsolute(Path storageRoot, String storageKey) {
        return storageRoot.resolve(storageKey).normalize();
    }

    private Author author(String fullName, String biography) {
        Author author = new Author();
        author.setFullName(fullName);
        author.setBiography(biography);
        return author;
    }

    private Category category(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return category;
    }

    private String adminEmail() {
        String email = System.getenv("BOOKWISE_ADMIN_EMAIL");
        return StringUtils.hasText(email) ? email.trim() : "admin@bookwise.local";
    }

    private String adminFullName() {
        String fullName = System.getenv("BOOKWISE_ADMIN_FULL_NAME");
        return StringUtils.hasText(fullName) ? fullName.trim() : "BookWise Administrator";
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
