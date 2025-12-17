package com.bot.business_through_awareness.repository;

import com.bot.business_through_awareness.model.Group;
import com.bot.business_through_awareness.service.GroupFileService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class GroupRepository {
    private final Map<Long, Group> groups = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final GroupFileService fileService;
    
    @Autowired
    public GroupRepository(GroupFileService fileService) {
        this.fileService = fileService;
    }
    
    @PostConstruct
    public void init() {
        // Загружаем группы из файла при старте
        List<Group> loadedGroups = fileService.loadGroups();
        
        for (Group group : loadedGroups) {
            groups.put(group.getId(), group);
            // Обновляем генератор ID, чтобы не было конфликтов
            if (group.getId() >= idGenerator.get()) {
                idGenerator.set(group.getId() + 1);
            }
        }
    }
    
    public List<Group> findAll() {
        return new ArrayList<>(groups.values());
    }
    
    public Optional<Group> findById(Long id) {
        return Optional.ofNullable(groups.get(id));
    }
    
    public Optional<Group> findByName(String name) {
        return groups.values().stream()
                .filter(g -> g.getName().equals(name))
                .findFirst();
    }
    
    public Group save(Group group) {
        if (group.getId() == null) {
            group.setId(idGenerator.getAndIncrement());
        }
        groups.put(group.getId(), group);
        saveToFile();
        return group;
    }
    
    public void deleteById(Long id) {
        groups.remove(id);
        saveToFile();
    }
    
    public void delete(Group group) {
        groups.remove(group.getId());
        saveToFile();
    }
    
    public long count() {
        return groups.size();
    }
    
    private void saveToFile() {
        fileService.saveGroups(new ArrayList<>(groups.values()));
    }
}


