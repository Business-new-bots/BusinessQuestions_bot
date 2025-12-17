package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Group;
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
public class GroupFileService {
    
    private final String groupsFilePath;
    private final ObjectMapper objectMapper;
    
    public GroupFileService(@Value("${groups.file.path:./groups.json}") String groupsFilePath) {
        this.groupsFilePath = groupsFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public List<Group> loadGroups() {
        File file = new File(groupsFilePath);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            
            List<Group> groups = objectMapper.readValue(
                    file,
                    new TypeReference<List<Group>>() {}
            );
            return groups != null ? groups : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке групп из файла: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void saveGroups(List<Group> groups) {
        try {
            // Создаем директорию, если её нет
            Path path = Paths.get(groupsFilePath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Сохраняем в файл
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(groupsFilePath), groups);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении групп в файл: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить группы в файл", e);
        }
    }
}


