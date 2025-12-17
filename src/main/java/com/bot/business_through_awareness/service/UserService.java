package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.User;
import com.bot.business_through_awareness.model.UserState;
import com.bot.business_through_awareness.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public User getOrCreateUser(Long id, String firstName, String lastName, String username) {
        return userRepository.findById(id)
                .orElseGet(() -> {
                    User user = new User(id, firstName);
                    user.setLastName(lastName);
                    user.setUsername(username);
                    return userRepository.save(user);
                });
    }
    
    public User updateUserState(Long userId, UserState state) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setState(state);
        return userRepository.save(user);
    }
    
    public User setCurrentCategory(Long userId, Long categoryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setCurrentCategoryId(categoryId);
        return userRepository.save(user);
    }
    
    public void setUserAsAdmin(Long userId, boolean isAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setIsAdmin(isAdmin);
        userRepository.save(user);
    }
    
    public List<User> getAllUsersByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public User setCurrentQuestion(Long userId, Long questionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setCurrentQuestionId(questionId);
        return userRepository.save(user);
    }
}

