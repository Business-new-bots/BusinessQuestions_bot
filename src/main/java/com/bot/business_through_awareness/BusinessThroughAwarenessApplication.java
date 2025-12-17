package com.bot.business_through_awareness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BusinessThroughAwarenessApplication {

	public static void main(String[] args) {
		System.out.println("Запуск приложения BusinessThroughAwareness...");
		System.out.println("Проверка переменных окружения:");
		String token = System.getenv("TELEGRAM_BOT_TOKEN");
		String username = System.getenv("TELEGRAM_BOT_USERNAME");
		System.out.println("TELEGRAM_BOT_TOKEN: " + (token != null && !token.isEmpty() ? "установлен" : "НЕ установлен!"));
		System.out.println("TELEGRAM_BOT_USERNAME: " + (username != null && !username.isEmpty() ? "установлен (" + username + ")" : "НЕ установлен!"));
		SpringApplication.run(BusinessThroughAwarenessApplication.class, args);
	}

}
