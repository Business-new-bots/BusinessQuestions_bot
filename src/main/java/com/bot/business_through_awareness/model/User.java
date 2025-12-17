package com.bot.business_through_awareness.model;

import java.time.LocalDateTime;

public class User {
    private Long id; // Telegram user ID
    private String firstName;
    private String lastName;
    private String username;
    private LocalDateTime registeredAt;
    private Long currentCategoryId; // Текущая выбранная категория
    private Long currentQuestionId; // Текущий вопрос, с которым работает пользователь (например, исполнитель отвечает)
    private UserState state = UserState.START; // Текущее состояние пользователя
    private Boolean isAdmin = false;
    
    public User() {
        this.registeredAt = LocalDateTime.now();
    }
    
    public User(Long id, String firstName) {
        this();
        this.id = id;
        this.firstName = firstName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
    
    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
    
    public Long getCurrentCategoryId() {
        return currentCategoryId;
    }
    
    public void setCurrentCategoryId(Long currentCategoryId) {
        this.currentCategoryId = currentCategoryId;
    }
    
    public Long getCurrentQuestionId() {
        return currentQuestionId;
    }
    
    public void setCurrentQuestionId(Long currentQuestionId) {
        this.currentQuestionId = currentQuestionId;
    }
    
    public UserState getState() {
        return state;
    }
    
    public void setState(UserState state) {
        this.state = state;
    }
    
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}

