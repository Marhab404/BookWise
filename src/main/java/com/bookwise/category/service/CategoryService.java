package com.bookwise.category.service;

import com.bookwise.category.dto.CategoryForm;
import com.bookwise.category.entity.Category;
import com.bookwise.category.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> listAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public Category create(CategoryForm form) {
        ensureUniqueName(form.getName(), null);
        Category category = new Category();
        map(category, form);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryForm form) {
        Category category = getById(id);
        ensureUniqueName(form.getName(), id);
        map(category, form);
        return categoryRepository.save(category);
    }

    private void ensureUniqueName(String name, Long currentId) {
        String normalized = name.trim();
        boolean exists = categoryRepository.existsByNameIgnoreCase(normalized);
        if (exists && (currentId == null || !getById(currentId).getName().equalsIgnoreCase(normalized))) {
            throw new IllegalArgumentException("Category name already exists");
        }
    }

    private void map(Category category, CategoryForm form) {
        category.setName(form.getName().trim());
        category.setDescription(form.getDescription());
    }
}
