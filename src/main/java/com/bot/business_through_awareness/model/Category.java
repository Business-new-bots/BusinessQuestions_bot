package com.bot.business_through_awareness.model;

import java.time.LocalDateTime;

public class Category {
    private Long id;
    private String name;
    private Long groupId; // ID группы, к которой относится категория
    private LocalDateTime createdAt;
    
    public Category() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Category(String name) {
        this();
        this.name = name;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}

