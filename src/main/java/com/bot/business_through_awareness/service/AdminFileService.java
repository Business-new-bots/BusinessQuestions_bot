package com.bot.business_through_awareness.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AdminFileService {
    
    private final String adminsFilePath;
    private final ObjectMapper objectMapper;
    
    public AdminFileService(@Value("${admins.file.path:./admins.json}") String adminsFilePath) {
        this.adminsFilePath = adminsFilePath;
        this.objectMapper = new ObjectMapper();
    }
    
    public List<String> loadAdmins() {
        File file = new File(adminsFilePath);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            
            List<String> admins = objectMapper.readValue(
                    file,
                    new TypeReference<List<String>>() {}
            );
            return admins != null ? admins : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке админов из файла: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void saveAdmins(List<String> admins) {
        try {
            // Создаем директорию, если её нет
            Path path = Paths.get(adminsFilePath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Сохраняем в файл
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(adminsFilePath), admins);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении админов в файл: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить админов в файл", e);
        }
    }
}


