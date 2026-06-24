package com.bookwise.user.controller;

import com.bookwise.security.AppUserPrincipal;
import com.bookwise.user.dto.CreateUserForm;
import com.bookwise.user.dto.EditUserForm;
import com.bookwise.user.entity.User;
import com.bookwise.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.listAll());
        return "admin/users/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("userForm", new CreateUserForm());
        model.addAttribute("formMode", "create");
        return "admin/users/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("userForm") CreateUserForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "admin/users/form";
        }
        try {
            userService.create(form);
            redirectAttributes.addFlashAttribute("flashMessage", "User created");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("user.failed", ex.getMessage());
            model.addAttribute("formMode", "create");
            return "admin/users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.getById(id);
        EditUserForm form = new EditUserForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole().name());
        model.addAttribute("userForm", form);
        model.addAttribute("user", user);
        model.addAttribute("formMode", "edit");
        return "admin/users/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") EditUserForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "edit");
            return "admin/users/form";
        }
        try {
            userService.update(id, form);
            redirectAttributes.addFlashAttribute("flashMessage", "User updated");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("user.failed", ex.getMessage());
            model.addAttribute("formMode", "edit");
            return "admin/users/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal AppUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id, principal.id());
            redirectAttributes.addFlashAttribute("flashMessage", "User deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("flashError", flashMessage(ex));
        }
        return "redirect:/admin/users";
    }

    private static String flashMessage(ResponseStatusException ex) {
        return ex.getReason() != null && !ex.getReason().isBlank() ? ex.getReason() : ex.getMessage();
    }
}
