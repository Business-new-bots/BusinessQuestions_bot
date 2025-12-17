package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Category;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryFileService {
    
    private final String categoriesFilePath;
    private final ObjectMapper objectMapper;
    
    public CategoryFileService(@Value("${categories.file.path:categories.json}") String categoriesFilePath) {
        this.categoriesFilePath = categoriesFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public List<Category> loadCategories() {
        File file = new File(categoriesFilePath);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            
            List<Category> categories = objectMapper.readValue(
                    file,
                    new TypeReference<List<Category>>() {}
            );
            return categories != null ? categories : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке категорий из файла: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void saveCategories(List<Category> categories) {
        try {
            // Создаем директорию, если её нет
            Path path = Paths.get(categoriesFilePath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Сохраняем в файл
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(categoriesFilePath), categories);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении категорий в файл: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить категории в файл", e);
        }
    }
}


