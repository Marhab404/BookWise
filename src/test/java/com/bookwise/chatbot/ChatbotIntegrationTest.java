package com.bookwise.chatbot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.bookwise.author.repository.AuthorRepository;
import com.bookwise.author.service.AuthorService;
import com.bookwise.book.repository.BookRepository;
import com.bookwise.book.service.BookService;
import com.bookwise.category.repository.CategoryRepository;
import com.bookwise.category.service.CategoryService;
import com.bookwise.chatbot.client.GeminiClient;
import com.bookwise.chatbot.client.GeminiClient.InteractionResponse;
import com.bookwise.chatbot.controller.ChatbotController.ConfirmRequest;
import com.bookwise.chatbot.controller.ChatbotController.MessageRequest;
import com.bookwise.chatbot.service.ChatbotService.ChatResponse;
import com.bookwise.chatbot.service.ChatbotService.PendingAction;
import com.bookwise.order.repository.OrderRepository;
import com.bookwise.payment.repository.PaymentRepository;
import com.bookwise.security.AppUserPrincipal;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ChatbotIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GeminiClient geminiClient;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private AuthorRepository authorRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private AuthorService authorService;

    @MockitoBean
    private CategoryService categoryService;

    private AppUserPrincipal adminPrincipal;
    private AppUserPrincipal readerPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = new AppUserPrincipal(1L, "Admin User", "admin@bookwise.com", UserRole.ADMIN);
        readerPrincipal = new AppUserPrincipal(2L, "Reader User", "reader@bookwise.com", UserRole.READER);
        
        // Mock default database queries
        when(bookRepository.findByPublishedTrueOrderByCreatedAtDesc()).thenReturn(List.of());
        when(bookRepository.findAll()).thenReturn(List.of());
        when(authorRepository.findAllByOrderByFullNameAsc()).thenReturn(List.of());
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        when(userRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());
        when(paymentRepository.findAll()).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(AppUserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    void testUnauthenticatedUserForbidden() throws Exception {
        MessageRequest request = new MessageRequest("Hello", null);
        mockMvc.perform(MockMvcRequestBuilders.post("/chatbot/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection()); // redirects to login since security entrypoint handles it
    }

    @Test
    void testReaderCatalogQuerySuccess() throws Exception {
        authenticate(readerPrincipal);

        String geminiResponse = """
            {
              "id": "interaction-r-1",
              "status": "completed",
              "steps": [
                {
                  "type": "model_output",
                  "content": [{ "type": "text", "text": "We have 0 books published currently." }]
                }
              ]
            }
            """;
        
        GeminiClient.InteractionResponse ir = objectMapper.readValue(geminiResponse, GeminiClient.InteractionResponse.class);
        when(geminiClient.postInteraction(anyString(), any())).thenReturn(ir);

        MessageRequest request = new MessageRequest("Show catalog size", null);
        mockMvc.perform(MockMvcRequestBuilders.post("/chatbot/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .principal(new UsernamePasswordAuthenticationToken(readerPrincipal, null, readerPrincipal.getAuthorities())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("We have 0 books published currently."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.actionPreview").isEmpty());

        // Verify that reader only calls catalog repositories and not admin-only repos
        verify(bookRepository, atLeastOnce()).findByPublishedTrueOrderByCreatedAtDesc();
        verify(userRepository, never()).findAll();
        verify(orderRepository, never()).findAll();
    }

    @Test
    void testAdminProposeWriteSuccess() throws Exception {
        authenticate(adminPrincipal);

        String geminiResponse = """
            {
              "id": "interaction-a-1",
              "status": "completed",
              "steps": [
                {
                  "type": "model_output",
                  "content": [
                    { 
                      "type": "text", 
                      "text": "I will propose creating a new author Robert C. Martin. [PROPOSED_ACTION] {\\\"action\\\": \\\"CREATE_AUTHOR\\\", \\\"params\\\": {\\\"fullName\\\": \\\"Robert C. Martin\\\", \\\"biography\\\": \\\"Uncle Bob\\\"}} [/PROPOSED_ACTION]" 
                    }
                  ]
                }
              ]
            }
            """;

        GeminiClient.InteractionResponse ir = objectMapper.readValue(geminiResponse, GeminiClient.InteractionResponse.class);
        when(geminiClient.postInteraction(anyString(), any())).thenReturn(ir);

        MessageRequest request = new MessageRequest("Create author Robert C. Martin", null);
        
        MockHttpSession session = new MockHttpSession();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/chatbot/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .session(session)
                .principal(new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("I will propose creating a new author Robert C. Martin."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.actionPreview.actionType").value("CREATE_AUTHOR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.actionPreview.actionId").exists())
                .andReturn();

        // Extract actionId and confirm execution
        String responseContent = result.getResponse().getContentAsString();
        ChatResponse chatResponse = objectMapper.readValue(responseContent, ChatResponse.class);
        String actionId = chatResponse.actionPreview().actionId();

        assertNotNull(actionId);
        assertNotNull(session.getAttribute("PENDING_ACTION_" + actionId));

        // Test Confirm Endpoint
        ConfirmRequest confirmRequest = new ConfirmRequest(actionId);
        mockMvc.perform(MockMvcRequestBuilders.post("/chatbot/actions/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest))
                .session(session)
                .principal(new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities())))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("success"));

        verify(authorService, times(1)).create(any());
        assertNull(session.getAttribute("PENDING_ACTION_" + actionId));
    }

    @Test
    void testReaderCannotConfirmAction() throws Exception {
        authenticate(adminPrincipal);

        // Stash a pending action in session
        MockHttpSession session = new MockHttpSession();
        String actionId = "action-uuid-123";
        PendingAction pendingAction = new PendingAction(
                actionId,
                "DELETE_AUTHOR",
                Map.of("id", 1L),
                Instant.now().plusSeconds(60)
        );
        session.setAttribute("PENDING_ACTION_" + actionId, pendingAction);

        // Try to confirm as reader
        authenticate(readerPrincipal);
        ConfirmRequest confirmRequest = new ConfirmRequest(actionId);
        mockMvc.perform(MockMvcRequestBuilders.post("/chatbot/actions/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmRequest))
                .session(session)
                .principal(new UsernamePasswordAuthenticationToken(readerPrincipal, null, readerPrincipal.getAuthorities())))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}
