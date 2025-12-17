package com.bot.business_through_awareness.repository;

import com.bot.business_through_awareness.model.Question;
import com.bot.business_through_awareness.model.QuestionStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class QuestionRepository {
    private final Map<Long, Question> questions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public Optional<Question> findById(Long id) {
        return Optional.ofNullable(questions.get(id));
    }
    
    public List<Question> findByStatus(QuestionStatus status) {
        return questions.values().stream()
                .filter(q -> q.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    public List<Question> findByUserId(Long userId) {
        return questions.values().stream()
                .filter(q -> q.getUser().getId().equals(userId))
                .collect(Collectors.toList());
    }
    
    public Question save(Question question) {
        if (question.getId() == null) {
            question.setId(idGenerator.getAndIncrement());
        }
        questions.put(question.getId(), question);
        return question;
    }
}
