package com.bookwise.user;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bookwise.security.AppUserPrincipal;
import com.bookwise.user.entity.User;
import com.bookwise.user.entity.UserRole;
import com.bookwise.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private AppUserPrincipal adminPrincipal;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        adminPrincipal = new AppUserPrincipal(1L, "Admin User", "admin@bookwise.com", UserRole.ADMIN);
        sampleUser = new User();
        sampleUser.setId(2L);
        sampleUser.setFullName("John Doe");
        sampleUser.setEmail("john@example.com");
        sampleUser.setRole(UserRole.READER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_shouldDisplayUsers() throws Exception {
        when(userService.listAll()).thenReturn(List.of(sampleUser));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    void createForm_shouldDisplayForm() throws Exception {
        mockMvc.perform(get("/admin/users/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attributeExists("userForm"))
                .andExpect(model().attribute("formMode", "create"));
    }

    @Test
    void create_shouldRedirectOnSuccess() throws Exception {
        when(userService.create(any())).thenReturn(sampleUser);

        mockMvc.perform(post("/admin/users/create")
                        .with(csrf())
                        .param("fullName", "John Doe")
                        .param("email", "john@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("role", "READER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    void create_shouldReRenderOnValidationError() throws Exception {
        mockMvc.perform(post("/admin/users/create")
                        .with(csrf())
                        .param("fullName", "")
                        .param("email", "bad")
                        .param("password", "")
                        .param("confirmPassword", "")
                        .param("role", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attribute("formMode", "create"));
    }

    @Test
    void create_shouldReRenderOnServiceError() throws Exception {
        when(userService.create(any())).thenThrow(new IllegalArgumentException("Email taken"));

        mockMvc.perform(post("/admin/users/create")
                        .with(csrf())
                        .param("fullName", "John")
                        .param("email", "taken@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("role", "READER"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attribute("formMode", "create"));
    }

    @Test
    void editForm_shouldDisplayPrefilledForm() throws Exception {
        when(userService.getById(2L)).thenReturn(sampleUser);

        mockMvc.perform(get("/admin/users/2/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/form"))
                .andExpect(model().attributeExists("userForm", "user"))
                .andExpect(model().attribute("formMode", "edit"));
    }

    @Test
    void update_shouldRedirectOnSuccess() throws Exception {
        when(userService.update(anyLong(), any())).thenReturn(sampleUser);

        mockMvc.perform(post("/admin/users/2/edit")
                        .with(csrf())
                        .param("fullName", "John Updated")
                        .param("email", "john@example.com")
                        .param("role", "READER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    void delete_shouldRedirectOnSuccess() throws Exception {
        doNothing().when(userService).delete(2L, 1L);

        mockMvc.perform(post("/admin/users/2/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashMessage"));
    }

    @Test
    void delete_shouldFlashErrorOnException() throws Exception {
        doThrow(new IllegalArgumentException("Cannot delete yourself"))
                .when(userService).delete(2L, 1L);

        mockMvc.perform(post("/admin/users/2/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashError"));
    }

    @Test
    void delete_shouldFlashErrorOnResponseStatusException() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(userService).delete(2L, 1L);

        mockMvc.perform(post("/admin/users/2/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("flashError"));
    }

    @Test
    void unauthenticated_shouldRedirectToLogin() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }
}
