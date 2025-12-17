package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Category;
import com.bot.business_through_awareness.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
    
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }
    
    public Optional<Category> getCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }
    
    public Category createCategory(String name, Long groupId) {
        if (categoryRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Категория с таким именем уже существует");
        }
        Category category = new Category(name);
        category.setGroupId(groupId);
        return categoryRepository.save(category);
    }
    
    public Category updateCategory(Long categoryId, String newName, Long newGroupId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
        
        // Проверяем, не занято ли новое имя другой категорией
        if (newName != null && !newName.equals(category.getName())) {
            if (categoryRepository.findByName(newName).isPresent()) {
                throw new IllegalArgumentException("Категория с таким именем уже существует");
            }
            category.setName(newName);
        }
        
        if (newGroupId != null) {
            category.setGroupId(newGroupId);
        }
        
        return categoryRepository.save(category);
    }
    
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
    
    public void deleteCategoryByName(String name) {
        categoryRepository.findByName(name).ifPresent(categoryRepository::delete);
    }
}

