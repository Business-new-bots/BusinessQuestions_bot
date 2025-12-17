package com.bot.business_through_awareness.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class BotInitializer implements CommandLineRunner {
    
    private final TelegramBotsApi telegramBotsApi;
    private final BusinessAwarenessBot bot;
    
    @Autowired
    public BotInitializer(TelegramBotsApi telegramBotsApi, BusinessAwarenessBot bot) {
        this.telegramBotsApi = telegramBotsApi;
        this.bot = bot;
    }
    
    @Override
    public void run(String... args) throws Exception {
        try {
            telegramBotsApi.registerBot(bot);
            System.out.println("Telegram bot успешно зарегистрирован!");
        } catch (TelegramApiException e) {
            System.err.println("Ошибка регистрации бота: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


