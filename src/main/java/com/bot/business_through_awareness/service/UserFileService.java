package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.User;
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
public class UserFileService {
    
    private final String usersFilePath;
    private final ObjectMapper objectMapper;
    
    public UserFileService(@Value("${users.file.path:./users.json}") String usersFilePath) {
        this.usersFilePath = usersFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public List<User> loadUsers() {
        File file = new File(usersFilePath);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            
            List<User> users = objectMapper.readValue(
                    file,
                    new TypeReference<List<User>>() {}
            );
            return users != null ? users : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке пользователей из файла: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void saveUsers(List<User> users) {
        try {
            // Создаем директорию, если её нет
            Path path = Paths.get(usersFilePath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Сохраняем в файл
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(usersFilePath), users);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении пользователей в файл: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить пользователей в файл", e);
        }
    }
}


