package com.bookwise.category.controller;

import com.bookwise.category.dto.CategoryForm;
import com.bookwise.category.service.CategoryService;
import jakarta.validation.Valid;
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
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.listAll());
        return "admin/categories/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute("formMode", "create");
        return "admin/categories/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            return "admin/categories/form";
        }
        try {
            categoryService.create(form);
            redirectAttributes.addFlashAttribute("flashMessage", "Category created");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("category.failed", ex.getMessage());
            model.addAttribute("formMode", "create");
            return "admin/categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var category = categoryService.getById(id);
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        model.addAttribute("categoryForm", form);
        model.addAttribute("category", category);
        model.addAttribute("formMode", "edit");
        return "admin/categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("categoryForm") CategoryForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "edit");
            return "admin/categories/form";
        }
        try {
            categoryService.update(id, form);
            redirectAttributes.addFlashAttribute("flashMessage", "Category updated");
            return "redirect:/admin/categories";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("category.failed", ex.getMessage());
            model.addAttribute("formMode", "edit");
            return "admin/categories/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("flashMessage", "Category deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("flashError", flashMessage(ex));
        }
        return "redirect:/admin/categories";
    }

    private String flashMessage(ResponseStatusException ex) {
        return ex.getReason() != null && !ex.getReason().isBlank() ? ex.getReason() : ex.getMessage();
    }
}
