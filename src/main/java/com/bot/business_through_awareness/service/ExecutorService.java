package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Executor;
import com.bot.business_through_awareness.repository.ExecutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExecutorService {
    
    private final ExecutorRepository executorRepository;
    
    @Autowired
    public ExecutorService(ExecutorRepository executorRepository) {
        this.executorRepository = executorRepository;
    }
    
    public List<Executor> getAllExecutors() {
        return executorRepository.findAll();
    }
    
    public Optional<Executor> getExecutorByUsername(String username) {
        return executorRepository.findByUsername(username);
    }
    
    public List<Executor> getExecutorsByGroupId(Long groupId) {
        return executorRepository.findByGroupId(groupId);
    }
    
    public Executor createExecutor(String username, Long groupId) {
        if (executorRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Исполнитель с таким username уже существует");
        }
        Executor executor = new Executor(username, groupId);
        return executorRepository.save(executor);
    }
    
    public void deleteExecutor(String username) {
        executorRepository.deleteByUsername(username);
    }
    
    public Executor updateExecutorGroup(String username, Long newGroupId) {
        Executor executor = executorRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Исполнитель не найден"));
        executor.setGroupId(newGroupId);
        return executorRepository.save(executor);
    }
}


