package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Category;
import com.bot.business_through_awareness.model.Question;
import com.bot.business_through_awareness.model.QuestionStatus;
import com.bot.business_through_awareness.model.User;
import com.bot.business_through_awareness.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {
    
    private final QuestionRepository questionRepository;
    
    @Autowired
    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }
    
    public Question createQuestion(User user, Category category, String questionText) {
        Question question = new Question();
        question.setUser(user);
        question.setCategory(category);
        question.setQuestionText(questionText);
        question.setStatus(QuestionStatus.PENDING);
        return questionRepository.save(question);
    }
    
    public List<Question> getPendingQuestions() {
        return questionRepository.findByStatus(QuestionStatus.PENDING);
    }
    
    public List<Question> getUserQuestions(Long userId) {
        return questionRepository.findByUserId(userId);
    }
    
    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }
    
    public Question answerQuestion(Long questionId, String answerText) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Вопрос не найден"));
        question.setAnswerText(answerText);
        question.setStatus(QuestionStatus.ANSWERED);
        question.setAnsweredAt(LocalDateTime.now());
        return questionRepository.save(question);
    }
    
    public Question save(Question question) {
        return questionRepository.save(question);
    }
}

