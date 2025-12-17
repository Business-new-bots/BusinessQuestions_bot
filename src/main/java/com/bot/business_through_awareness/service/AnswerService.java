package com.bot.business_through_awareness.service;

import com.bot.business_through_awareness.model.Question;
import com.bot.business_through_awareness.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AnswerService {
    
    private final QuestionService questionService;
    private final QuestionRepository questionRepository;
    
    @Autowired
    public AnswerService(QuestionService questionService, QuestionRepository questionRepository) {
        this.questionService = questionService;
        this.questionRepository = questionRepository;
    }
    
    public Question sendAnswerToUser(Long questionId, String answerText) {
        Question question = questionService.answerQuestion(questionId, answerText);
        // Ответ будет отправлен через бота
        // Этот метод можно расширить для интеграции с отправкой сообщений
        return question;
    }
    
    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }
}

