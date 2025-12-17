package com.bot.business_through_awareness.model;

import java.time.LocalDateTime;

public class Question {
    private Long id;
    private User user;
    private Category category;
    private String questionText;
    private String answerText;
    private LocalDateTime askedAt;
    private LocalDateTime answeredAt;
    private QuestionStatus status = QuestionStatus.PENDING;
    // Username исполнителя, которому назначен этот вопрос (telegram username без @)
    private String executorUsername;
    
    public Question() {
        this.askedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public void setCategory(Category category) {
        this.category = category;
    }
    
    public String getQuestionText() {
        return questionText;
    }
    
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
    
    public String getAnswerText() {
        return answerText;
    }
    
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
    
    public LocalDateTime getAskedAt() {
        return askedAt;
    }
    
    public void setAskedAt(LocalDateTime askedAt) {
        this.askedAt = askedAt;
    }
    
    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }
    
    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
    
    public QuestionStatus getStatus() {
        return status;
    }
    
    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
    
    public String getExecutorUsername() {
        return executorUsername;
    }
    
    public void setExecutorUsername(String executorUsername) {
        this.executorUsername = executorUsername;
    }
}

