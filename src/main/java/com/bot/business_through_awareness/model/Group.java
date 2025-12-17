package com.bot.business_through_awareness.model;

import java.time.LocalDateTime;

public class Group {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    
    public Group() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Group(String name) {
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
}


