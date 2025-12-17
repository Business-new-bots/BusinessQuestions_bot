package com.bot.business_through_awareness.repository;

import com.bot.business_through_awareness.model.Executor;
import com.bot.business_through_awareness.service.ExecutorFileService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class ExecutorRepository {
    private final Map<String, Executor> executors = new ConcurrentHashMap<>(); // Key: username
    private final ExecutorFileService fileService;
    
    @Autowired
    public ExecutorRepository(ExecutorFileService fileService) {
        this.fileService = fileService;
    }
    
    @PostConstruct
    public void init() {
        // Загружаем исполнителей из файла при старте
        List<Executor> loadedExecutors = fileService.loadExecutors();
        
        for (Executor executor : loadedExecutors) {
            executors.put(executor.getUsername(), executor);
        }
    }
    
    public List<Executor> findAll() {
        return new ArrayList<>(executors.values());
    }
    
    public Optional<Executor> findByUsername(String username) {
        return Optional.ofNullable(executors.get(username));
    }
    
    public List<Executor> findByGroupId(Long groupId) {
        return executors.values().stream()
                .filter(e -> e.getGroupId() != null && e.getGroupId().equals(groupId))
                .collect(Collectors.toList());
    }
    
    public Executor save(Executor executor) {
        System.out.println("Сохранение исполнителя в репозиторий: username=" + executor.getUsername() + ", groupId=" + executor.getGroupId());
        executors.put(executor.getUsername(), executor);
        saveToFile();
        System.out.println("Исполнитель сохранен, всего исполнителей: " + executors.size());
        return executor;
    }
    
    public void deleteByUsername(String username) {
        executors.remove(username);
        saveToFile();
    }
    
    public void delete(Executor executor) {
        executors.remove(executor.getUsername());
        saveToFile();
    }
    
    public long count() {
        return executors.size();
    }
    
    private void saveToFile() {
        try {
            System.out.println("Сохранение исполнителей в файл, количество: " + executors.size());
            fileService.saveExecutors(new ArrayList<>(executors.values()));
            System.out.println("Исполнители успешно сохранены в файл");
        } catch (Exception e) {
            System.err.println("Ошибка при сохранении исполнителей в файл: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

