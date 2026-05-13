package com.bookwise.book.controller;

import com.bookwise.author.service.AuthorService;
import com.bookwise.book.dto.CreateBookForm;
import com.bookwise.book.dto.UpdateBookForm;
import com.bookwise.book.entity.Book;
import com.bookwise.book.service.BookService;
import com.bookwise.category.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/admin/books")
public class AdminBookController {

    private final BookService bookService;
    private final AuthorService authorService;
    private final CategoryService categoryService;

    public AdminBookController(BookService bookService, AuthorService authorService, CategoryService categoryService) {
        this.bookService = bookService;
        this.authorService = authorService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("books", bookService.listAllBooks());
        return "admin/books/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("bookForm", new CreateBookForm());
        model.addAttribute("formMode", "create");
        populateReferenceData(model);
        return "admin/books/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("bookForm") CreateBookForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            populateReferenceData(model);
            return "admin/books/form";
        }
        try {
            bookService.create(form);
            redirectAttributes.addFlashAttribute("flashMessage", "Book created");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("book.failed", ex.getMessage());
            model.addAttribute("formMode", "create");
            populateReferenceData(model);
            return "admin/books/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Book book = bookService.getAdminBook(id);
        UpdateBookForm form = new UpdateBookForm();
        form.setTitle(book.getTitle());
        form.setSubtitle(book.getSubtitle());
        form.setDescription(book.getDescription());
        form.setIsbn(book.getIsbn());
        form.setPublicationYear(book.getPublicationYear());
        form.setLanguage(book.getLanguage());
        form.setPriceAmount(java.math.BigDecimal.valueOf(book.getPriceAmount(), 2));
        form.setCurrency(book.getCurrency());
        form.setPublished(book.isPublished());
        form.setAuthorIds(book.getAuthors().stream().map(com.bookwise.author.entity.Author::getId).toList());
        form.setCategoryIds(book.getCategories().stream().map(com.bookwise.category.entity.Category::getId).toList());
        model.addAttribute("book", book);
        model.addAttribute("bookForm", form);
        model.addAttribute("formMode", "edit");
        populateReferenceData(model);
        return "admin/books/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("bookForm") UpdateBookForm form, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "edit");
            populateReferenceData(model);
            return "admin/books/form";
        }
        try {
            bookService.update(id, form);
            redirectAttributes.addFlashAttribute("flashMessage", "Book updated");
            return "redirect:/admin/books";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("book.failed", ex.getMessage());
            model.addAttribute("formMode", "edit");
            populateReferenceData(model);
            return "admin/books/form";
        }
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookService.publish(id, true);
        redirectAttributes.addFlashAttribute("flashMessage", "Book published");
        return "redirect:/admin/books";
    }

    @PostMapping("/{id}/unpublish")
    public String unpublish(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookService.publish(id, false);
        redirectAttributes.addFlashAttribute("flashMessage", "Book unpublished");
        return "redirect:/admin/books";
    }

    private void populateReferenceData(Model model) {
        model.addAttribute("allAuthors", authorService.listAll());
        model.addAttribute("allCategories", categoryService.listAll());
    }
}
