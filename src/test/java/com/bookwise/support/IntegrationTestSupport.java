package com.bookwise.support;

import com.bookwise.author.entity.Author;
import com.bookwise.author.repository.AuthorRepository;
import com.bookwise.book.dto.CreateBookForm;
import com.bookwise.book.entity.Book;
import com.bookwise.book.repository.BookRepository;
import com.bookwise.book.service.BookService;
import com.bookwise.category.entity.Category;
import com.bookwise.category.repository.CategoryRepository;
import com.bookwise.config.StorageProperties;
import com.bookwise.order.entity.OrderItem;
import com.bookwise.order.entity.OrderStatus;
import com.bookwise.order.entity.PurchaseOrder;
import com.bookwise.order.repository.OrderRepository;
import com.bookwise.payment.entity.Payment;
import com.bookwise.payment.entity.PaymentMethod;
import com.bookwise.payment.entity.PaymentStatus;
import com.bookwise.payment.repository.PaymentRepository;
import com.bookwise.security.JwtService;
import com.bookwise.user.entity.User;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.repository.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected UserRepository userRepository;
    @Autowired protected AuthorRepository authorRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected BookRepository bookRepository;
    @Autowired protected OrderRepository orderRepository;
    @Autowired protected PaymentRepository paymentRepository;
    @Autowired protected BookService bookService;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;
    @Autowired protected StorageProperties storageProperties;

    protected static final byte[] COVER_BYTES = """
            <svg xmlns="http://www.w3.org/2000/svg" width="600" height="900" viewBox="0 0 600 900">
              <rect width="600" height="900" fill="#0f766e"/>
              <text x="60" y="160" fill="#ffffff" font-family="Arial" font-size="40">BookWise</text>
            </svg>
            """.getBytes(StandardCharsets.UTF_8);

    protected static final byte[] PDF_BYTES = "%PDF-1.4\nBookWise demo file".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void cleanStorage() throws IOException {
        Path root = Path.of(storageProperties.root()).toAbsolutePath().normalize();
        if (Files.exists(root)) {
            Files.walk(root)
                    .sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            if (!path.equals(root)) {
                                Files.deleteIfExists(path);
                            }
                        } catch (IOException ignored) {
                        }
                    });
        }
        Files.createDirectories(root);
    }

    protected User createUser(String fullName, String email, String password, UserRole role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    protected Author createAuthor(String fullName) {
        Author author = new Author();
        author.setFullName(fullName);
        author.setBiography(fullName + " biography");
        return authorRepository.save(author);
    }

    protected Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(name + " description");
        return categoryRepository.save(category);
    }

    protected Book createBook(String title, long priceMinor, boolean published) {
        Author author = authorRepository.findAll().stream().findFirst().orElseGet(() -> createAuthor("Author " + UUID.randomUUID()));
        Category category = categoryRepository.findAll().stream().findFirst().orElseGet(() -> createCategory("Category " + UUID.randomUUID()));

        CreateBookForm form = new CreateBookForm();
        form.setTitle(title);
        form.setSubtitle(title + " subtitle");
        form.setDescription(title + " description");
        form.setIsbn("ISBN-" + UUID.randomUUID());
        form.setPublicationYear(2025);
        form.setLanguage("English");
        form.setPriceAmount(BigDecimal.valueOf(priceMinor, 2));
        form.setCurrency("MAD");
        form.setPublished(published);
        form.setAuthorIds(List.of(author.getId()));
        form.setCategoryIds(List.of(category.getId()));
        form.setCoverImage(new MockMultipartFile("coverImage", "cover.svg", "image/svg+xml", COVER_BYTES));
        form.setDigitalFiles(new MultipartFile[]{
                new MockMultipartFile("digitalFiles", title.replace(' ', '-').toLowerCase() + ".pdf", "application/pdf", PDF_BYTES)
        });
        return bookService.create(form);
    }

    protected PurchaseOrder createPaidOwnership(User user, Book book) {
        PurchaseOrder order = new PurchaseOrder();
        order.setUser(user);
        order.setStatus(OrderStatus.PAID);
        order.setCurrency(book.getCurrency());
        order.setTotalAmount(book.getPriceAmount());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setBook(book);
        item.setUnitPriceAmount(book.getPriceAmount());
        order.setItems(List.of(item));
        PurchaseOrder savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setMethod(PaymentMethod.BANK_TRANSFER);
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setReceiptFilePath("proofs/placeholder.pdf");
        paymentRepository.save(payment);

        savedOrder.setPayment(payment);
        return savedOrder;
    }

    protected org.springframework.mock.web.MockHttpServletResponse login(String email, String password) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("email", email)
                        .param("password", password))
                .andReturn()
                .getResponse();
    }

    protected jakarta.servlet.http.Cookie jwtCookieFromLogin(String email, String password) throws Exception {
        String setCookie = login(email, password).getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        return new jakarta.servlet.http.Cookie("BOOKWISE_JWT", value);
    }
}
