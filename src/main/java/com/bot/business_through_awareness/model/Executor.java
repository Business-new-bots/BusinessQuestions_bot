package com.bot.business_through_awareness.model;

import java.time.LocalDateTime;

public class Executor {
    private String username; // Telegram username без @
    private Long groupId; // ID группы, к которой привязан исполнитель
    private LocalDateTime createdAt;
    
    public Executor() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Executor(String username, Long groupId) {
        this();
        this.username = username;
        this.groupId = groupId;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}


