package com.bookwise.author.controller;

import com.bookwise.author.dto.AuthorForm;
import com.bookwise.author.service.AuthorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/authors")
public class AdminAuthorController {

    private final AuthorService authorService;

    public AdminAuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("authors", authorService.listAll());
        return "admin/authors/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("authorForm", new AuthorForm());
        model.addAttribute("formMode", "create");
        return "admin/authors/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("authorForm") AuthorForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "admin/authors/form";
        }
        try {
            authorService.create(form);
            redirectAttributes.addFlashAttribute("flashMessage", "Author created");
            return "redirect:/admin/authors";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("author.failed", ex.getMessage());
            model.addAttribute("formMode", "create");
            return "admin/authors/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var author = authorService.getById(id);
        AuthorForm form = new AuthorForm();
        form.setFullName(author.getFullName());
        form.setBiography(author.getBiography());
        model.addAttribute("authorForm", form);
        model.addAttribute("author", author);
        model.addAttribute("formMode", "edit");
        return "admin/authors/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("authorForm") AuthorForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "edit");
            return "admin/authors/form";
        }
        try {
            authorService.update(id, form);
            redirectAttributes.addFlashAttribute("flashMessage", "Author updated");
            return "redirect:/admin/authors";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("author.failed", ex.getMessage());
            model.addAttribute("formMode", "edit");
            return "admin/authors/form";
        }
    }
}
