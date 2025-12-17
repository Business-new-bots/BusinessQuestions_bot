package com.bot.business_through_awareness.repository;

import com.bot.business_through_awareness.model.Category;
import com.bot.business_through_awareness.service.CategoryFileService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CategoryRepository {
    private final Map<Long, Category> categories = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final CategoryFileService fileService;
    
    @Autowired
    public CategoryRepository(CategoryFileService fileService) {
        this.fileService = fileService;
    }
    
    @PostConstruct
    public void init() {
        // Загружаем категории из файла при старте
        List<Category> loadedCategories = fileService.loadCategories();
        
        for (Category category : loadedCategories) {
            categories.put(category.getId(), category);
            // Обновляем генератор ID, чтобы не было конфликтов
            if (category.getId() >= idGenerator.get()) {
                idGenerator.set(category.getId() + 1);
            }
        }
    }
    
    public List<Category> findAll() {
        return new ArrayList<>(categories.values());
    }
    
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(categories.get(id));
    }
    
    public Optional<Category> findByName(String name) {
        return categories.values().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }
    
    public Category save(Category category) {
        if (category.getId() == null) {
            category.setId(idGenerator.getAndIncrement());
        }
        categories.put(category.getId(), category);
        saveToFile();
        return category;
    }
    
    public void deleteById(Long id) {
        categories.remove(id);
        saveToFile();
    }
    
    public void delete(Category category) {
        categories.remove(category.getId());
        saveToFile();
    }
    
    public long count() {
        return categories.size();
    }
    
    private void saveToFile() {
        fileService.saveCategories(new ArrayList<>(categories.values()));
    }
}
