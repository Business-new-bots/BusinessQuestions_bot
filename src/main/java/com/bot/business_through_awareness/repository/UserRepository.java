package com.bot.business_through_awareness.repository;

import com.bot.business_through_awareness.model.User;
import com.bot.business_through_awareness.service.UserFileService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class UserRepository {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final UserFileService fileService;
    
    @Autowired
    public UserRepository(UserFileService fileService) {
        this.fileService = fileService;
    }
    
    @PostConstruct
    public void init() {
        // Загружаем пользователей из файла при старте
        List<User> loadedUsers = fileService.loadUsers();
        
        for (User user : loadedUsers) {
            users.put(user.getId(), user);
        }
    }
    
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }
    
    public User save(User user) {
        users.put(user.getId(), user);
        saveToFile();
        return user;
    }
    
    public boolean existsById(Long id) {
        return users.containsKey(id);
    }
    
    public List<User> findByUsername(String username) {
        if (username == null) {
            return new ArrayList<>();
        }
        return users.values().stream()
                .filter(u -> username != null && username.equals(u.getUsername()))
                .collect(Collectors.toList());
    }
    
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
    
    private void saveToFile() {
        fileService.saveUsers(new ArrayList<>(users.values()));
    }
}
