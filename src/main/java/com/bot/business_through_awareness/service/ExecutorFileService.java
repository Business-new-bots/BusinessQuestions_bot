package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Executor;
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
public class ExecutorFileService {
    
    private final String executorsFilePath;
    private final ObjectMapper objectMapper;
    
    public ExecutorFileService(@Value("${executors.file.path:./executor.json}") String executorsFilePath) {
        this.executorsFilePath = executorsFilePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public List<Executor> loadExecutors() {
        File file = new File(executorsFilePath);
        
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try {
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            
            List<Executor> executors = objectMapper.readValue(
                    file,
                    new TypeReference<List<Executor>>() {}
            );
            return executors != null ? executors : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Ошибка при загрузке исполнителей из файла: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void saveExecutors(List<Executor> executors) {
        try {
            // Создаем директорию, если её нет
            Path path = Paths.get(executorsFilePath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // Сохраняем в файл
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(executorsFilePath), executors);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении исполнителей в файл: " + e.getMessage());
            throw new RuntimeException("Не удалось сохранить исполнителей в файл", e);
        }
    }
}


