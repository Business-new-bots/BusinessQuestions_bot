package com.bot.business_through_awareness.bot;

import com.bot.business_through_awareness.model.*;
import com.bot.business_through_awareness.service.AdminFileService;
import com.bot.business_through_awareness.service.CategoryService;
import com.bot.business_through_awareness.service.ExecutorService;
import com.bot.business_through_awareness.service.GroupService;
import com.bot.business_through_awareness.service.QuestionService;
import com.bot.business_through_awareness.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class BusinessAwarenessBot extends TelegramLongPollingBot {
    
    private final String botToken;
    private final String botUsername;
    private List<String> adminUsernames;
    
    private final UserService userService;
    private final CategoryService categoryService;
    private final GroupService groupService;
    private final ExecutorService executorService;
    private final QuestionService questionService;
    private final AdminFileService adminFileService;
    
    public BusinessAwarenessBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.bot.admin-usernames:}") String adminUsernamesStr,
            UserService userService,
            CategoryService categoryService,
            GroupService groupService,
            ExecutorService executorService,
            QuestionService questionService,
            AdminFileService adminFileService) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userService = userService;
        this.categoryService = categoryService;
        this.groupService = groupService;
        this.executorService = executorService;
        this.questionService = questionService;
        this.adminFileService = adminFileService;
        
        // Загружаем админов из файла, если файл существует, иначе из конфига
        List<String> fileAdmins = adminFileService.loadAdmins();
        if (!fileAdmins.isEmpty()) {
            this.adminUsernames = fileAdmins;
        } else {
            this.adminUsernames = parseAdminUsernames(adminUsernamesStr);
            // Сохраняем в файл при первом запуске
            if (!this.adminUsernames.isEmpty()) {
                adminFileService.saveAdmins(this.adminUsernames);
            }
        }
    }
    
    private List<String> parseAdminUsernames(String adminUsernamesStr) {
        if (adminUsernamesStr == null || adminUsernamesStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(adminUsernamesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith("@") ? s.substring(1) : s) // Убираем @ если есть
                .collect(Collectors.toList());
    }
    
    private void reloadAdmins() {
        this.adminUsernames = adminFileService.loadAdmins();
    }
    
    @Override
    public String getBotUsername() {
        return botUsername;
    }
    
    @Override
    public String getBotToken() {
        return botToken;
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }
    
    private void handleMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();
        Long userId = update.getMessage().getFrom().getId();
        String firstName = update.getMessage().getFrom().getFirstName();
        String lastName = update.getMessage().getFrom().getLastName();
        String username = update.getMessage().getFrom().getUserName();
        
        // Получаем или создаем пользователя
        User user = userService.getOrCreateUser(userId, firstName, lastName, username);
        
        // Проверяем, является ли пользователь админом (по username)
        // Перезагружаем список админов перед проверкой
        reloadAdmins();
        if (username != null && adminUsernames.contains(username)) {
            userService.setUserAsAdmin(userId, true);
            user = userService.getUserById(userId).orElse(user);
        } else if (username != null) {
            // Если username не в списке админов, сбрасываем права
            userService.setUserAsAdmin(userId, false);
            user = userService.getUserById(userId).orElse(user);
        }
        
        // Обработка команды /start или кнопок "Начать"/"Задать вопрос"
        if (messageText.equals("/start") || messageText.equals("Начать") || messageText.equals("Задать вопрос")) {
            userService.updateUserState(userId, UserState.START);
            userService.setCurrentCategory(userId, null);
            sendWelcomeMessage(chatId, user);
            return;
        }
        
        // Обработка кнопки AdminMenu (Reply кнопка)
        if (messageText.equals("AdminMenu")) {
            if (!user.getIsAdmin()) {
                sendMessage(chatId, "У вас нет прав для доступа к админ-меню.");
                return;
            }
            handleAdminMenu(chatId, userId);
            return;
        }
        
        // Обработка команд админа (старый способ, для совместимости)
        if (messageText.startsWith("/admin")) {
            if (!user.getIsAdmin()) {
                sendMessage(chatId, "У вас нет прав для выполнения этой команды.");
                return;
            }
            handleAdminCommand(chatId, messageText, user);
            return;
        }
        
        // Обработка обычных сообщений
        UserState state = user.getState();
        
        if (state == UserState.WAITING_FOR_QUESTION) {
            handleQuestion(chatId, userId, messageText, user);
        } else if (state == UserState.EXECUTOR_ANSWERING_QUESTION) {
            handleExecutorAnswer(chatId, userId, messageText, user);
        } else if (state == UserState.ADMIN_ADDING_CATEGORY && user.getIsAdmin()) {
            // Сохраняем название категории и запрашиваем выбор группы
            userService.setCurrentCategory(userId, -1L); // Временное хранение названия через отрицательный ID
            handleAdminSelectGroupForNewCategory(chatId, userId, messageText);
        } else if (state == UserState.ADMIN_ADDING_GROUP && user.getIsAdmin()) {
            // Добавление новой группы
            try {
                groupService.createGroup(messageText);
                sendMessage(chatId, "Группа \"" + messageText + "\" успешно добавлена!");
                userService.updateUserState(userId, UserState.START);
                sendWelcomeMessage(chatId, user);
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка: " + e.getMessage());
            }
        } else if (state == UserState.ADMIN_EDITING_CATEGORY && user.getIsAdmin()) {
            // Редактирование названия категории
            Long categoryId = user.getCurrentCategoryId();
            if (categoryId != null && categoryId > 0) {
                try {
                    Category category = categoryService.getCategoryById(categoryId).orElse(null);
                    if (category != null) {
                        categoryService.updateCategory(categoryId, messageText, null);
                        sendMessage(chatId, "Название категории успешно изменено на \"" + messageText + "\"!");
                        userService.updateUserState(userId, UserState.START);
                        userService.setCurrentCategory(userId, null);
                        sendWelcomeMessage(chatId, user);
                    }
                } catch (Exception e) {
                    sendMessage(chatId, "Ошибка: " + e.getMessage());
                }
            }
        } else if (state == UserState.ADMIN_ADDING_EXECUTOR && user.getIsAdmin()) {
            // Сохраняем username исполнителя и запрашиваем выбор группы
            String executorUsername = messageText.trim();
            if (executorUsername.startsWith("@")) {
                executorUsername = executorUsername.substring(1);
            }
            handleAdminSelectGroupForExecutor(chatId, userId, executorUsername);
        } else if (state == UserState.ADMIN_MODE && user.getIsAdmin()) {
            // Обработка ввода username админа
            String input = messageText.trim();
            if (input.startsWith("@") || input.matches("^[a-zA-Z0-9_]+$")) {
                // Это username админа
                String adminUsername = input.startsWith("@") ? input.substring(1) : input;
                handleAdminAddAdminInput(chatId, userId, adminUsername);
            }
        } else {
            // Если пользователь не в состоянии ожидания вопроса, показываем приветствие
            sendWelcomeMessage(chatId, user);
        }
    }
    
    private void handleCallbackQuery(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String callbackData = update.getCallbackQuery().getData();
        Long userId = update.getCallbackQuery().getFrom().getId();
        String firstName = update.getCallbackQuery().getFrom().getFirstName();
        String lastName = update.getCallbackQuery().getFrom().getLastName();
        String username = update.getCallbackQuery().getFrom().getUserName();
        
        // Создаем пользователя, если его еще нет (важно для callback-запросов)
        // Это нужно, потому что пользователь может нажать на кнопку категории
        // до того, как отправит любое сообщение боту
        User user = userService.getOrCreateUser(userId, firstName, lastName, username);
        
        System.out.println("Обработка callback: " + callbackData);
        
        if (callbackData.equals("show_categories")) {
            handleShowCategories(chatId);
        } else if (callbackData.startsWith("exec_answer_")) {
            // Исполнитель хочет ответить на конкретный вопрос
            Long questionId;
            try {
                questionId = Long.parseLong(callbackData.substring("exec_answer_".length()));
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Неверный идентификатор вопроса.");
                return;
            }
            
            // Проверяем, что этот пользователь является исполнителем (по username)
            if (username == null || executorService.getExecutorByUsername(username).isEmpty()) {
                sendMessage(chatId, "У вас нет прав отвечать на вопросы как исполнитель.");
                return;
            }
            
            // Сохраняем в пользователе идентификатор вопроса и переводим в состояние ответа
            userService.setCurrentQuestion(userId, questionId);
            userService.updateUserState(userId, UserState.EXECUTOR_ANSWERING_QUESTION);
            
            Question question = questionService.getQuestionById(questionId).orElse(null);
            if (question == null) {
                sendMessage(chatId, "Вопрос не найден или уже был удален.");
                userService.updateUserState(userId, UserState.START);
                userService.setCurrentQuestion(userId, null);
                return;
            }
            
            String categoryName = question.getCategory() != null ? question.getCategory().getName() : "";
            StringBuilder text = new StringBuilder();
            text.append("Вы выбрали ответ на вопрос #").append(questionId).append(".\n\n");
            if (!categoryName.isEmpty()) {
                text.append("Тема: ").append(categoryName).append("\n\n");
            }
            text.append("Вопрос:\n").append(question.getQuestionText()).append("\n\n");
            text.append("Пожалуйста, отправьте ваш ответ одним сообщением.");
            
            sendMessage(chatId, text.toString());
        } else if (callbackData.startsWith("category_")) {
            Long categoryId = Long.parseLong(callbackData.substring("category_".length()));
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            handleCategorySelection(chatId, userId, categoryId, messageId);
        } else if (callbackData.equals("admin_menu")) {
            handleAdminMenu(chatId, userId);
        } else if (callbackData.equals("admin_add_category")) {
            handleAdminAddCategory(chatId, userId);
        } else if (callbackData.equals("admin_delete_category")) {
            handleAdminDeleteCategory(chatId, userId);
        } else if (callbackData.equals("admin_list_categories")) {
            handleAdminListCategories(chatId);
        } else if (callbackData.equals("admin_list_groups")) {
            handleAdminListGroups(chatId);
        } else if (callbackData.equals("admin_list_admins")) {
            handleAdminListAdmins(chatId);
        } else if (callbackData.equals("admin_add_executor")) {
            handleAdminAddExecutor(chatId, userId);
        } else if (callbackData.equals("admin_delete_executor")) {
            handleAdminDeleteExecutor(chatId, userId);
        } else if (callbackData.equals("admin_list_executors")) {
            handleAdminListExecutors(chatId);
        } else if (callbackData.equals("admin_edit_category")) {
            handleAdminEditCategory(chatId, userId);
        } else if (callbackData.equals("admin_add_group")) {
            handleAdminAddGroup(chatId, userId);
        } else if (callbackData.equals("admin_delete_group")) {
            handleAdminDeleteGroup(chatId, userId);
        } else if (callbackData.equals("admin_add_admin")) {
            handleAdminAddAdmin(chatId, userId);
        } else if (callbackData.equals("admin_delete_admin")) {
            handleAdminDeleteAdmin(chatId, userId);
        } else if (callbackData.equals("admin_back")) {
            if (user != null) {
                sendWelcomeMessage(chatId, user);
            }
        } else if (callbackData.startsWith("admin_delete_")) {
            Long categoryId = Long.parseLong(callbackData.substring("admin_delete_".length()));
            handleAdminDeleteCategoryById(chatId, categoryId);
        } else if (callbackData.startsWith("admin_remove_admin_")) {
            String usernameToRemove = callbackData.substring("admin_remove_admin_".length());
            handleAdminRemoveAdmin(chatId, usernameToRemove);
        } else if (callbackData.startsWith("admin_select_group_executor_")) {
            // Формат: admin_select_group_executor_{groupId}_username_{username}
            // ВАЖНО: Эта проверка должна быть ПЕРЕД admin_select_group_, так как она более специфична
            System.out.println("Обработка выбора группы для исполнителя, callback: " + callbackData);
            String data = callbackData.substring("admin_select_group_executor_".length());
            System.out.println("Данные после удаления префикса: " + data);
            String[] parts = data.split("_username_");
            System.out.println("Количество частей после split: " + parts.length);
            for (int i = 0; i < parts.length; i++) {
                System.out.println("Часть " + i + ": " + parts[i]);
            }
            if (parts.length == 2) {
                try {
                    Long groupId = Long.parseLong(parts[0]);
                    String executorUsername = parts[1];
                    System.out.println("Парсинг успешен: groupId=" + groupId + ", username=" + executorUsername);
                    handleAdminCreateExecutorWithGroup(chatId, userId, executorUsername, groupId);
                } catch (NumberFormatException e) {
                    System.err.println("Ошибка парсинга groupId из callback: " + callbackData);
                    System.err.println("Исключение: " + e.getMessage());
                    e.printStackTrace();
                    sendMessage(chatId, "Ошибка: не удалось обработать выбор группы.");
                } catch (Exception e) {
                    System.err.println("Неожиданная ошибка при обработке выбора группы: " + e.getMessage());
                    e.printStackTrace();
                    sendMessage(chatId, "Ошибка: " + e.getMessage());
                }
            } else {
                System.err.println("Неверный формат callback для выбора группы исполнителя: " + callbackData);
                System.err.println("Ожидалось 2 части, получено: " + parts.length);
                sendMessage(chatId, "Ошибка: неверный формат данных.");
            }
        } else if (callbackData.startsWith("admin_select_group_")) {
            // Формат: admin_select_group_{groupId}_name_{categoryName}
            String data = callbackData.substring("admin_select_group_".length());
            String[] parts = data.split("_name_");
            if (parts.length == 2) {
                Long groupId = Long.parseLong(parts[0]);
                String categoryName = parts[1];
                handleAdminCreateCategoryWithGroup(chatId, userId, categoryName, groupId);
            }
        } else if (callbackData.startsWith("admin_edit_cat_")) {
            Long categoryId = Long.parseLong(callbackData.substring("admin_edit_cat_".length()));
            handleAdminEditCategorySelect(chatId, userId, categoryId);
        } else if (callbackData.startsWith("admin_change_group_menu_")) {
            Long categoryId = Long.parseLong(callbackData.substring("admin_change_group_menu_".length()));
            handleAdminChangeGroupMenu(chatId, userId, categoryId);
        } else if (callbackData.startsWith("admin_change_group_")) {
            String[] parts = callbackData.substring("admin_change_group_".length()).split("_");
            Long categoryId = Long.parseLong(parts[0]);
            Long groupId = Long.parseLong(parts[1]);
            handleAdminChangeCategoryGroup(chatId, userId, categoryId, groupId);
        } else if (callbackData.startsWith("admin_rename_cat_")) {
            Long categoryId = Long.parseLong(callbackData.substring("admin_rename_cat_".length()));
            handleAdminRenameCategory(chatId, userId, categoryId);
        } else if (callbackData.startsWith("admin_delete_group_")) {
            Long groupId = Long.parseLong(callbackData.substring("admin_delete_group_".length()));
            handleAdminDeleteGroupById(chatId, groupId);
        } else if (callbackData.startsWith("admin_remove_executor_")) {
            String usernameToRemove = callbackData.substring("admin_remove_executor_".length());
            handleAdminRemoveExecutor(chatId, usernameToRemove);
        }
    }
    
    private void sendWelcomeMessage(Long chatId, User user) {
        String welcomeText = "Привет! Осознанность и энергия — фундамент твоего процветания. Спрашивай, поможем!";
        
        // Создаем сообщение с кнопкой "Выбрать категорию"
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(welcomeText);
        
        // Добавляем Inline кнопку "Выбрать категорию"
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Выбрать категорию");
        button.setCallbackData("show_categories");
        row.add(button);
        keyboard.add(row);
        
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        
        // Отправляем Reply кнопки (появляются под чатом) - сразу после приветствия
        // Это заменит стандартную кнопку "СТАРТ" от Telegram
        sendReplyKeyboard(chatId, user.getIsAdmin());
    }
    
    private void handleShowCategories(Long chatId) {
        List<Category> categories = categoryService.getAllCategories();
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите категорию:");
        
        // Inline кнопки для категорий
        if (!categories.isEmpty()) {
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            
            for (Category category : categories) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(category.getName());
                button.setCallbackData("category_" + category.getId());
                row.add(button);
                keyboard.add(row);
            }
            
            keyboardMarkup.setKeyboard(keyboard);
            message.setReplyMarkup(keyboardMarkup);
        } else {
            message.setText("Категории отсутствуют");
        }
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendReplyKeyboard(Long chatId, boolean isAdmin) {
        // Создаем клавиатуру с кнопкой "Задать вопрос" для всех пользователей
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false); // Клавиатура остается видимой
        replyKeyboardMarkup.setSelective(false);
        
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        
        // Кнопка "Задать вопрос" для всех пользователей (дублирует поведение /start)
        row.add(new KeyboardButton("Задать вопрос"));
        
        // Кнопка "AdminMenu" только для админов
        if (isAdmin) {
            row.add(new KeyboardButton("AdminMenu"));
        }
        
        keyboardRows.add(row);
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        
        // Отправляем сообщение с клавиатурой.
        // Telegram требует, чтобы текст сообщения был непустым, поэтому отображаем
        // краткую подсказку. Это сообщение останется в чате, а клавиатура будет закреплена.
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Можешь в любой момент нажать «Задать вопрос» ниже, чтобы начать новый диалог.");
        message.setReplyMarkup(replyKeyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleCategorySelection(Long chatId, Long userId, Long categoryId, Integer messageId) {
        // Подсвечиваем выбранную категорию в inline-клавиатуре
        highlightSelectedCategory(chatId, messageId, categoryId);
        
        categoryService.getCategoryById(categoryId).ifPresent(category -> {
            userService.setCurrentCategory(userId, categoryId);
            userService.updateUserState(userId, UserState.WAITING_FOR_QUESTION);
            
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Отлично, теперь задай свой вопрос");
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void handleQuestion(Long chatId, Long userId, String questionText, User user) {
        if (user.getCurrentCategoryId() == null) {
            sendWelcomeMessage(chatId, user);
            return;
        }
        
        categoryService.getCategoryById(user.getCurrentCategoryId()).ifPresent(category -> {
            // Создаем вопрос
            Question question = questionService.createQuestion(user, category, questionText);
            
            // Отправляем подтверждение клиенту
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("Спасибо, совсем скоро вернусь к тебе с ответом!");
            
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            
            // Находим исполнителей по группе категории и случайным образом выбираем одного
            if (category.getGroupId() != null) {
                List<Executor> executors = executorService.getExecutorsByGroupId(category.getGroupId());
                
                if (!executors.isEmpty()) {
                    // Случайным образом выбираем одного исполнителя из списка
                    Random random = new Random();
                    Executor selectedExecutor = executors.get(random.nextInt(executors.size()));
                    
                    // Формируем текст сообщения для исполнителя
                    String executorMessage = "📩 Новый вопрос\n\n" +
                            "Категория: " + category.getName() + "\n" +
                            "От: " + (user.getFirstName() != null ? user.getFirstName() : "") +
                            (user.getLastName() != null ? " " + user.getLastName() : "") +
                            (user.getUsername() != null ? " (@" + user.getUsername() + ")" : "") + "\n" +
                            "ID вопроса: #" + question.getId() + "\n\n" +
                            "Вопрос:\n" + questionText;
                    
                    // Ищем пользователя-исполнителя по username в нашей базе
                    List<User> executorUsers = userService.getAllUsersByUsername(selectedExecutor.getUsername());
                    
                    if (!executorUsers.isEmpty()) {
                        // Если исполнитель найден в базе (уже писал боту), отправляем ему сообщение
                        User executorUser = executorUsers.get(0);
                        
                        // Сохраняем username исполнителя в вопросе, чтобы знать, кто должен отвечать
                        question.setExecutorUsername(selectedExecutor.getUsername());
                        questionService.save(question);
                        
                        try {
                            SendMessage executorMsg = new SendMessage();
                            executorMsg.setChatId(executorUser.getId().toString());
                            executorMsg.setText(executorMessage);
                            
                            // Добавляем кнопку "Ответить" для исполнителя
                            InlineKeyboardMarkup executorKeyboard = new InlineKeyboardMarkup();
                            List<List<InlineKeyboardButton>> execKeyboardRows = new ArrayList<>();
                            List<InlineKeyboardButton> execRow = new ArrayList<>();
                            InlineKeyboardButton replyButton = new InlineKeyboardButton();
                            replyButton.setText("💬 Ответить");
                            replyButton.setCallbackData("exec_answer_" + question.getId());
                            execRow.add(replyButton);
                            execKeyboardRows.add(execRow);
                            executorMsg.setReplyMarkup(executorKeyboard);
                            executorKeyboard.setKeyboard(execKeyboardRows);
                            
                            execute(executorMsg);
                            System.out.println("Вопрос #" + question.getId() + " отправлен исполнителю @" + selectedExecutor.getUsername() + " (ID: " + executorUser.getId() + ")");
                        } catch (TelegramApiException e) {
                            System.err.println("Ошибка при отправке вопроса исполнителю @" + selectedExecutor.getUsername() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        // Исполнитель еще не писал боту, поэтому мы не знаем его chatId
                        // Логируем это - когда исполнитель напишет боту, можно будет отправить ему накопленные вопросы
                        System.out.println("Исполнитель @" + selectedExecutor.getUsername() + " еще не зарегистрирован в боте. " +
                                "Вопрос #" + question.getId() + " будет отправлен после того, как исполнитель напишет боту /start");
                    }
                } else {
                    System.out.println("Для группы категории \"" + category.getName() + "\" не найдено исполнителей. " +
                            "Вопрос #" + question.getId() + " не может быть передан.");
                }
            } else {
                System.out.println("Категория \"" + category.getName() + "\" не привязана к группе. " +
                        "Вопрос #" + question.getId() + " не может быть передан.");
            }
            
            // Сбрасываем состояние пользователя
            userService.updateUserState(userId, UserState.START);
            userService.setCurrentCategory(userId, null);
            
            // Показываем кнопку "Задать вопрос" после отправки вопроса
            User updatedUser = userService.getUserById(userId).orElse(user);
            sendReplyKeyboard(chatId, updatedUser.getIsAdmin());
        });
    }
    
    private void highlightSelectedCategory(Long chatId, Integer messageId, Long selectedCategoryId) {
        List<Category> categories = categoryService.getAllCategories();
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Category category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            
            String name = category.getName();
            if (category.getId() != null && category.getId().equals(selectedCategoryId)) {
                // Добавляем голубой маркер к выбранной категории
                name = "🔹 " + name;
            }
            
            button.setText(name);
            button.setCallbackData("category_" + category.getId());
            row.add(button);
            keyboard.add(row);
        }
        
        keyboardMarkup.setKeyboard(keyboard);
        
        EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
        edit.setChatId(chatId.toString());
        edit.setMessageId(messageId);
        edit.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            // Если не удалось отредактировать (например, старое сообщение), просто логируем
            e.printStackTrace();
        }
    }
    
    private void handleExecutorAnswer(Long chatId, Long userId, String answerText, User executorUser) {
        Long questionId = executorUser.getCurrentQuestionId();
        if (questionId == null) {
            sendMessage(chatId, "Не удалось определить, на какой вопрос вы отвечаете. Пожалуйста, нажмите кнопку \"Ответить\" под нужным вопросом.");
            userService.updateUserState(userId, UserState.START);
            if (executorUser.getIsAdmin() != null && executorUser.getIsAdmin()) {
                sendReplyKeyboard(chatId, true);
            }
            return;
        }
        
        Question question = questionService.getQuestionById(questionId).orElse(null);
        if (question == null) {
            sendMessage(chatId, "Вопрос не найден или уже был удален.");
            userService.updateUserState(userId, UserState.START);
            userService.setCurrentQuestion(userId, null);
            if (executorUser.getIsAdmin() != null && executorUser.getIsAdmin()) {
                sendReplyKeyboard(chatId, true);
            }
            return;
        }
        
        // Проверяем, действительно ли этот исполнитель назначен на вопрос (по username)
        String executorUsername = executorUser.getUsername();
        if (executorUsername == null || question.getExecutorUsername() == null || 
                !executorUsername.equalsIgnoreCase(question.getExecutorUsername())) {
            sendMessage(chatId, "Этот вопрос назначен другому исполнителю.");
            userService.updateUserState(userId, UserState.START);
            userService.setCurrentQuestion(userId, null);
            if (executorUser.getIsAdmin() != null && executorUser.getIsAdmin()) {
                sendReplyKeyboard(chatId, true);
            }
            return;
        }
        
        // Отмечаем вопрос как отвеченный и сохраняем текст ответа
        questionService.answerQuestion(questionId, answerText);
        
        // Отправляем ответ пользователю
        User originalUser = question.getUser();
        if (originalUser != null) {
            Long targetChatId = originalUser.getId();
            String categoryName = question.getCategory() != null ? question.getCategory().getName() : "";
            
            StringBuilder response = new StringBuilder();
            response.append("✨ Ответ на ваш вопрос");
            if (!categoryName.isEmpty()) {
                response.append(" по теме \"").append(categoryName).append("\"");
            }
            response.append(":\n\n");
            response.append("Ваш вопрос:\n").append(question.getQuestionText()).append("\n\n");
            response.append("Ответ:\n").append(answerText);
            
            sendMessage(targetChatId, response.toString());
        }
        
        // Уведомляем исполнителя
        sendMessage(chatId, "Ответ отправлен пользователю.");
        
        // Сбрасываем состояние исполнителя
        userService.updateUserState(userId, UserState.START);
        userService.setCurrentQuestion(userId, null);
        if (executorUser.getIsAdmin() != null && executorUser.getIsAdmin()) {
            sendReplyKeyboard(chatId, true);
        }
    }
    
    private void handleAdminCommand(Long chatId, String command, User user) {
        // Теперь админ-команды доступны только через меню
        handleAdminMenu(chatId, user.getId());
    }
    
    private void handleAdminMenu(Long chatId, Long userId) {
        User user = userService.getUserById(userId).orElse(null);
        if (user == null || !user.getIsAdmin()) {
            sendMessage(chatId, "У вас нет прав для доступа к админ-меню.");
            return;
        }
        
        String menuText = "Админ-меню:\n\nВыберите действие:";
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопка "Добавить категорию"
        List<InlineKeyboardButton> addRow = new ArrayList<>();
        InlineKeyboardButton addButton = new InlineKeyboardButton();
        addButton.setText("➕ Добавить категорию");
        addButton.setCallbackData("admin_add_category");
        addRow.add(addButton);
        keyboard.add(addRow);
        
        // Кнопка "Удалить категорию"
        List<InlineKeyboardButton> deleteRow = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("➖ Удалить категорию");
        deleteButton.setCallbackData("admin_delete_category");
        deleteRow.add(deleteButton);
        keyboard.add(deleteRow);
        
        // Кнопка "Изменить категорию"
        List<InlineKeyboardButton> editRow = new ArrayList<>();
        InlineKeyboardButton editButton = new InlineKeyboardButton();
        editButton.setText("✏️ Изменить категорию");
        editButton.setCallbackData("admin_edit_category");
        editRow.add(editButton);
        keyboard.add(editRow);
        
        // Кнопка "Список категорий"
        List<InlineKeyboardButton> listRow = new ArrayList<>();
        InlineKeyboardButton listButton = new InlineKeyboardButton();
        listButton.setText("📋 Список категорий");
        listButton.setCallbackData("admin_list_categories");
        listRow.add(listButton);
        keyboard.add(listRow);
        
        // Кнопка "Добавить группу"
        List<InlineKeyboardButton> addGroupRow = new ArrayList<>();
        InlineKeyboardButton addGroupButton = new InlineKeyboardButton();
        addGroupButton.setText("➕ Добавить группу");
        addGroupButton.setCallbackData("admin_add_group");
        addGroupRow.add(addGroupButton);
        keyboard.add(addGroupRow);
        
        // Кнопка "Удалить группу"
        List<InlineKeyboardButton> deleteGroupRow = new ArrayList<>();
        InlineKeyboardButton deleteGroupButton = new InlineKeyboardButton();
        deleteGroupButton.setText("➖ Удалить группу");
        deleteGroupButton.setCallbackData("admin_delete_group");
        deleteGroupRow.add(deleteGroupButton);
        keyboard.add(deleteGroupRow);
        
        // Кнопка "Список групп"
        List<InlineKeyboardButton> listGroupsRow = new ArrayList<>();
        InlineKeyboardButton listGroupsButton = new InlineKeyboardButton();
        listGroupsButton.setText("📋 Список групп");
        listGroupsButton.setCallbackData("admin_list_groups");
        listGroupsRow.add(listGroupsButton);
        keyboard.add(listGroupsRow);
        
        // Кнопка "Добавить админа"
        List<InlineKeyboardButton> addAdminRow = new ArrayList<>();
        InlineKeyboardButton addAdminButton = new InlineKeyboardButton();
        addAdminButton.setText("➕ Добавить админа");
        addAdminButton.setCallbackData("admin_add_admin");
        addAdminRow.add(addAdminButton);
        keyboard.add(addAdminRow);
        
        // Кнопка "Удалить админа"
        List<InlineKeyboardButton> deleteAdminRow = new ArrayList<>();
        InlineKeyboardButton deleteAdminButton = new InlineKeyboardButton();
        deleteAdminButton.setText("➖ Удалить админа");
        deleteAdminButton.setCallbackData("admin_delete_admin");
        deleteAdminRow.add(deleteAdminButton);
        keyboard.add(deleteAdminRow);
        
        // Кнопка "Список админов"
        List<InlineKeyboardButton> listAdminsRow = new ArrayList<>();
        InlineKeyboardButton listAdminsButton = new InlineKeyboardButton();
        listAdminsButton.setText("📋 Список админов");
        listAdminsButton.setCallbackData("admin_list_admins");
        listAdminsRow.add(listAdminsButton);
        keyboard.add(listAdminsRow);
        
        // Кнопка "Добавить исполнителя"
        List<InlineKeyboardButton> addExecutorRow = new ArrayList<>();
        InlineKeyboardButton addExecutorButton = new InlineKeyboardButton();
        addExecutorButton.setText("➕ Добавить исполнителя");
        addExecutorButton.setCallbackData("admin_add_executor");
        addExecutorRow.add(addExecutorButton);
        keyboard.add(addExecutorRow);
        
        // Кнопка "Удалить исполнителя"
        List<InlineKeyboardButton> deleteExecutorRow = new ArrayList<>();
        InlineKeyboardButton deleteExecutorButton = new InlineKeyboardButton();
        deleteExecutorButton.setText("➖ Удалить исполнителя");
        deleteExecutorButton.setCallbackData("admin_delete_executor");
        deleteExecutorRow.add(deleteExecutorButton);
        keyboard.add(deleteExecutorRow);
        
        // Кнопка "Список исполнителей"
        List<InlineKeyboardButton> listExecutorsRow = new ArrayList<>();
        InlineKeyboardButton listExecutorsButton = new InlineKeyboardButton();
        listExecutorsButton.setText("📋 Список исполнителей");
        listExecutorsButton.setCallbackData("admin_list_executors");
        listExecutorsRow.add(listExecutorsButton);
        keyboard.add(listExecutorsRow);
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(menuText);
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminAddCategory(Long chatId, Long userId) {
        userService.updateUserState(userId, UserState.ADMIN_ADDING_CATEGORY);
        sendMessage(chatId, "Введите название новой категории:");
    }
    
    private void handleAdminSelectGroupForNewCategory(Long chatId, Long userId, String categoryName) {
        List<Group> groups = groupService.getAllGroups();
        if (groups.isEmpty()) {
            sendMessage(chatId, "Сначала создайте группу! Используйте \"Добавить группу\" в админ-меню.");
            userService.updateUserState(userId, UserState.START);
            return;
        }
        
        // Сохраняем название категории во временном хранилище (используем отрицательный ID как флаг)
        // В реальности нужно хранить это в User или использовать Map для временных данных
        // Для простоты используем currentCategoryId как флаг, а название передадим через callback
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Group group : groups) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(group.getName());
            button.setCallbackData("admin_select_group_" + group.getId() + "_name_" + categoryName);
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите группу для категории \"" + categoryName + "\":");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminDeleteCategory(Long chatId, Long userId) {
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            sendMessage(chatId, "Категории отсутствуют");
            return;
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Category category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("❌ " + category.getName());
            button.setCallbackData("admin_delete_" + category.getId());
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите категорию для удаления:");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminListCategories(Long chatId) {
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            sendMessage(chatId, "Категории отсутствуют");
        } else {
            String list = categories.stream()
                    .map(c -> {
                        String groupName = groupService.getGroupById(c.getGroupId())
                                .map(Group::getName)
                                .orElse("Не указана");
                        return "- " + c.getName() + " (Группа: " + groupName + ")";
                    })
                    .collect(Collectors.joining("\n"));
            sendMessage(chatId, "Список категорий:\n" + list);
        }
    }
    
    private void handleAdminListGroups(Long chatId) {
        List<Group> groups = groupService.getAllGroups();
        if (groups.isEmpty()) {
            sendMessage(chatId, "Группы отсутствуют");
        } else {
            String list = groups.stream()
                    .map(g -> {
                        long categoriesCount = categoryService.getAllCategories().stream()
                                .filter(c -> c.getGroupId() != null && c.getGroupId().equals(g.getId()))
                                .count();
                        return "- " + g.getName() + " (Категорий: " + categoriesCount + ")";
                    })
                    .collect(Collectors.joining("\n"));
            sendMessage(chatId, "Список групп:\n" + list);
        }
    }
    
    private void handleAdminListAdmins(Long chatId) {
        reloadAdmins(); // Обновляем список админов перед отображением
        if (adminUsernames.isEmpty()) {
            sendMessage(chatId, "Админы отсутствуют");
        } else {
            String list = adminUsernames.stream()
                    .map(username -> "- @" + username)
                    .collect(Collectors.joining("\n"));
            sendMessage(chatId, "Список админов:\n" + list);
        }
    }
    
    private void handleAdminDeleteCategoryById(Long chatId, Long categoryId) {
        categoryService.getCategoryById(categoryId).ifPresent(category -> {
            try {
                categoryService.deleteCategory(categoryId);
                sendMessage(chatId, "Категория \"" + category.getName() + "\" успешно удалена!");
            } catch (Exception e) {
                sendMessage(chatId, "Ошибка: " + e.getMessage());
            }
        });
    }
    
    private void handleAdminAddAdmin(Long chatId, Long userId) {
        userService.updateUserState(userId, UserState.ADMIN_MODE);
        sendMessage(chatId, "Введите username нового админа (без @):");
    }
    
    private void handleAdminAddAdminInput(Long chatId, Long userId, String username) {
        try {
            if (adminUsernames.contains(username)) {
                sendMessage(chatId, "Админ с username \"" + username + "\" уже существует!");
                userService.updateUserState(userId, UserState.START);
                User user = userService.getUserById(userId).orElse(null);
                if (user != null) {
                    sendWelcomeMessage(chatId, user);
                }
                return;
            }
            
            adminUsernames.add(username);
            adminFileService.saveAdmins(adminUsernames);
            reloadAdmins();
            
            // Обновляем статус админа для пользователя, если он добавил себя
            userService.getUserById(userId).ifPresent(u -> {
                if (u.getUsername() != null && u.getUsername().equals(username)) {
                    userService.setUserAsAdmin(userId, true);
                }
            });
            
            sendMessage(chatId, "Админ \"" + username + "\" успешно добавлен!");
            userService.updateUserState(userId, UserState.START);
            User user = userService.getUserById(userId).orElse(null);
            if (user != null) {
                sendWelcomeMessage(chatId, user);
            }
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
    
    private void handleAdminDeleteAdmin(Long chatId, Long userId) {
        if (adminUsernames.isEmpty()) {
            sendMessage(chatId, "Список админов пуст");
            return;
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (String adminUsername : adminUsernames) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("❌ @" + adminUsername);
            button.setCallbackData("admin_remove_admin_" + adminUsername);
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите админа для удаления:");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminRemoveAdmin(Long chatId, String username) {
        try {
            if (!adminUsernames.contains(username)) {
                sendMessage(chatId, "Админ \"" + username + "\" не найден!");
                return;
            }
            
            if (adminUsernames.size() == 1) {
                sendMessage(chatId, "Нельзя удалить последнего админа!");
                return;
            }
            
            adminUsernames.remove(username);
            adminFileService.saveAdmins(adminUsernames);
            reloadAdmins();
            
            // Убираем права админа у всех пользователей с таким username
            List<User> usersWithUsername = userService.getAllUsersByUsername(username);
            for (User u : usersWithUsername) {
                userService.setUserAsAdmin(u.getId(), false);
            }
            
            sendMessage(chatId, "Админ \"" + username + "\" успешно удален!");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
    
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminCreateCategoryWithGroup(Long chatId, Long userId, String categoryName, Long groupId) {
        try {
            categoryService.createCategory(categoryName, groupId);
            sendMessage(chatId, "Категория \"" + categoryName + "\" успешно добавлена!");
            userService.updateUserState(userId, UserState.START);
            userService.setCurrentCategory(userId, null);
            User user = userService.getUserById(userId).orElse(null);
            if (user != null) {
                sendWelcomeMessage(chatId, user);
            }
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
    
    private void handleAdminAddGroup(Long chatId, Long userId) {
        userService.updateUserState(userId, UserState.ADMIN_ADDING_GROUP);
        sendMessage(chatId, "Введите название новой группы:");
    }
    
    private void handleAdminDeleteGroup(Long chatId, Long userId) {
        List<Group> groups = groupService.getAllGroups();
        if (groups.isEmpty()) {
            sendMessage(chatId, "Группы отсутствуют");
            return;
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Group group : groups) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("❌ " + group.getName());
            button.setCallbackData("admin_delete_group_" + group.getId());
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите группу для удаления:");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminDeleteGroupById(Long chatId, Long groupId) {
        try {
            // Проверяем, есть ли категории в этой группе
            List<Category> categoriesInGroup = categoryService.getAllCategories().stream()
                    .filter(c -> c.getGroupId() != null && c.getGroupId().equals(groupId))
                    .collect(Collectors.toList());
            
            if (!categoriesInGroup.isEmpty()) {
                sendMessage(chatId, "Нельзя удалить группу, в которой есть категории! Сначала удалите или переместите категории.");
                return;
            }
            
            groupService.getGroupById(groupId).ifPresent(group -> {
                groupService.deleteGroup(groupId);
                sendMessage(chatId, "Группа \"" + group.getName() + "\" успешно удалена!");
            });
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
    
    private void handleAdminEditCategory(Long chatId, Long userId) {
        List<Category> categories = categoryService.getAllCategories();
        if (categories.isEmpty()) {
            sendMessage(chatId, "Категории отсутствуют");
            return;
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Category category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("✏️ " + category.getName());
            button.setCallbackData("admin_edit_cat_" + category.getId());
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите категорию для редактирования:");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminEditCategorySelect(Long chatId, Long userId, Long categoryId) {
        Category category = categoryService.getCategoryById(categoryId).orElse(null);
        if (category == null) {
            sendMessage(chatId, "Категория не найдена");
            return;
        }
        
        // Сохраняем ID категории для редактирования
        userService.setCurrentCategory(userId, categoryId);
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        // Кнопка "Изменить название"
        List<InlineKeyboardButton> renameRow = new ArrayList<>();
        InlineKeyboardButton renameButton = new InlineKeyboardButton();
        renameButton.setText("✏️ Изменить название");
        renameButton.setCallbackData("admin_rename_cat_" + categoryId);
        renameRow.add(renameButton);
        keyboard.add(renameRow);
        
        // Кнопка "Изменить группу"
        List<InlineKeyboardButton> changeGroupRow = new ArrayList<>();
        InlineKeyboardButton changeGroupButton = new InlineKeyboardButton();
        changeGroupButton.setText("🔄 Изменить группу");
        changeGroupButton.setCallbackData("admin_change_group_menu_" + categoryId);
        changeGroupRow.add(changeGroupButton);
        keyboard.add(changeGroupRow);
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        String groupName = groupService.getGroupById(category.getGroupId())
                .map(Group::getName)
                .orElse("Не указана");
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Категория: " + category.getName() + "\nГруппа: " + groupName + "\n\nЧто хотите изменить?");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminRenameCategory(Long chatId, Long userId, Long categoryId) {
        userService.setCurrentCategory(userId, categoryId);
        userService.updateUserState(userId, UserState.ADMIN_EDITING_CATEGORY);
        sendMessage(chatId, "Введите новое название категории:");
    }
    
    private void handleAdminChangeGroupMenu(Long chatId, Long userId, Long categoryId) {
        List<Group> groups = groupService.getAllGroups();
        if (groups.isEmpty()) {
            sendMessage(chatId, "Группы отсутствуют");
            return;
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Group group : groups) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(group.getName());
            button.setCallbackData("admin_change_group_" + categoryId + "_" + group.getId());
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите новую группу для категории:");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminChangeCategoryGroup(Long chatId, Long userId, Long categoryId, Long groupId) {
        try {
            categoryService.updateCategory(categoryId, null, groupId);
            Group group = groupService.getGroupById(groupId).orElse(null);
            String groupName = group != null ? group.getName() : "Неизвестная";
            sendMessage(chatId, "Группа категории успешно изменена на \"" + groupName + "\"!");
            userService.updateUserState(userId, UserState.START);
            userService.setCurrentCategory(userId, null);
            User user = userService.getUserById(userId).orElse(null);
            if (user != null) {
                sendWelcomeMessage(chatId, user);
            }
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
    
    private void handleAdminAddExecutor(Long chatId, Long userId) {
        userService.updateUserState(userId, UserState.ADMIN_ADDING_EXECUTOR);
        sendMessage(chatId, "Введите username исполнителя (без @):");
    }
    
    private void handleAdminSelectGroupForExecutor(Long chatId, Long userId, String username) {
        List<Group> groups = groupService.getAllGroups();
        if (groups.isEmpty()) {
            sendMessage(chatId, "Сначала создайте группу! Используйте \"Добавить группу\" в админ-меню.");
            userService.updateUserState(userId, UserState.START);
            return;
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Group group : groups) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(group.getName());
            button.setCallbackData("admin_select_group_executor_" + group.getId() + "_username_" + username);
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите группу для исполнителя @" + username + ":");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminCreateExecutorWithGroup(Long chatId, Long userId, String username, Long groupId) {
        try {
            System.out.println("Добавление исполнителя: username=" + username + ", groupId=" + groupId);
            executorService.createExecutor(username, groupId);
            Group group = groupService.getGroupById(groupId).orElse(null);
            String groupName = group != null ? group.getName() : "Неизвестная";
            sendMessage(chatId, "Исполнитель @" + username + " успешно добавлен в группу \"" + groupName + "\"!");
            userService.updateUserState(userId, UserState.START);
            User user = userService.getUserById(userId).orElse(null);
            if (user != null) {
                sendWelcomeMessage(chatId, user);
            }
            System.out.println("Исполнитель успешно добавлен и сохранен в файл");
        } catch (Exception e) {
            System.err.println("Ошибка при добавлении исполнителя: " + e.getMessage());
            e.printStackTrace();
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
    
    private void handleAdminDeleteExecutor(Long chatId, Long userId) {
        List<Executor> executors = executorService.getAllExecutors();
        if (executors.isEmpty()) {
            sendMessage(chatId, "Исполнители отсутствуют");
            return;
        }
        
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        
        for (Executor executor : executors) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            String groupName = groupService.getGroupById(executor.getGroupId())
                    .map(Group::getName)
                    .orElse("Неизвестная");
            button.setText("❌ @" + executor.getUsername() + " (" + groupName + ")");
            button.setCallbackData("admin_remove_executor_" + executor.getUsername());
            row.add(button);
            keyboard.add(row);
        }
        
        // Кнопка "Назад"
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀️ Назад");
        backButton.setCallbackData("admin_back");
        backRow.add(backButton);
        keyboard.add(backRow);
        
        keyboardMarkup.setKeyboard(keyboard);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите исполнителя для удаления:");
        message.setReplyMarkup(keyboardMarkup);
        
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void handleAdminRemoveExecutor(Long chatId, String username) {
        try {
            executorService.deleteExecutor(username);
            sendMessage(chatId, "Исполнитель @" + username + " успешно удален!");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }
    
    private void handleAdminListExecutors(Long chatId) {
        List<Executor> executors = executorService.getAllExecutors();
        if (executors.isEmpty()) {
            sendMessage(chatId, "Исполнители отсутствуют");
        } else {
            String list = executors.stream()
                    .map(e -> {
                        String groupName = groupService.getGroupById(e.getGroupId())
                                .map(Group::getName)
                                .orElse("Не указана");
                        return "- @" + e.getUsername() + " (Группа: " + groupName + ")";
                    })
                    .collect(Collectors.joining("\n"));
            sendMessage(chatId, "Список исполнителей:\n" + list);
        }
    }
}

