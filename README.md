# Telegram Bot - Бизнес через осознанность

Telegram бот для вопросов и ответов по бизнесу и осознанности.

## Функциональность

- Приветствие при входе в бота
- Выбор категории из списка
- Задание вопроса по выбранной категории
- Получение ответа от администратора
- Управление категориями (только для админов)

## Категории

Бот включает следующие категории:
- Стратегия и планирование
- Работа с клиентами и коммуникации
- Управление и лидерство
- Бизнес-модели и маркетинг
- Кризисное управление
- Финансы и инвестиции
- Психология и мотивация
- Осознанность и медитация
- Личностное развитие и навыки
- Тренды и инновации в бизнесе
- Баланс работы и личной жизни

## Настройка

### 1. Создание бота в Telegram

1. Откройте [@BotFather](https://t.me/botfather) в Telegram
2. Отправьте команду `/newbot`
3. Следуйте инструкциям для создания бота
4. Сохраните полученный токен

### 2. Настройка конфигурации

Отредактируйте файл `src/main/resources/application.yaml`:

```yaml
telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN:your-bot-token-here}
    username: ${TELEGRAM_BOT_USERNAME:your-bot-username}
    admin-ids: ${TELEGRAM_ADMIN_IDS:} # Список ID админов через запятую
```

Или установите переменные окружения:
- `TELEGRAM_BOT_TOKEN` - токен бота от BotFather
- `TELEGRAM_BOT_USERNAME` - username бота (без @)
- `TELEGRAM_ADMIN_USERNAMES` - список username администраторов через запятую (например: `admin1,another_admin`)

### 3. Настройка администраторов

Администраторы определяются по username (без символа @):
- В `application.yaml`
- Можно указать несколько админов через запятую: `admin2,admin3`
- Или через переменную окружения: `export TELEGRAM_ADMIN_USERNAMES=admin1,admin2`

## Запуск

### Сборка проекта

```bash
mvn clean install
```

### Запуск приложения

```bash
mvn spring-boot:run
```

Или через IDE запустите класс `BusinessThroughAwarenessApplication`

## Использование

### Для пользователей

1. Найдите бота в Telegram по username
2. Нажмите `/start` или откройте бота
3. Выберите категорию из списка
4. Задайте свой вопрос
5. Дождитесь ответа от администратора

### Команды администратора

Администраторы могут использовать следующие команды:

- `/admin` - показать список команд
- `/admin add_category <название>` - добавить новую категорию
- `/admin delete_category <название>` - удалить категорию
- `/admin list_categories` - показать список всех категорий
- `/admin questions` - показать список вопросов, ожидающих ответа
- `/admin answer <id> <ответ>` - ответить на вопрос с указанным ID

Пример ответа на вопрос:
```
/admin answer 1 Ваш ответ на вопрос пользователя
```

## Хранение данных

### Категории

Категории хранятся в JSON файле `categories.json` (по умолчанию в корне проекта). 

**Как это работает:**
- Категории хранятся в файле `categories.json` в корне проекта
- При старте приложения категории загружаются из файла
- При добавлении/удалении категории через команды `/admin` файл автоматически обновляется
- Файл `categories.json` включен в проект и содержит 11 стандартных категорий

**Настройка пути к файлу:**

В `application.yaml` можно указать свой путь:
```yaml
categories:
  file:
    path: ${CATEGORIES_FILE_PATH:categories.json}
```

Или через переменную окружения:
```bash
export CATEGORIES_FILE_PATH=/path/to/categories.json
```

**Формат файла:**
```json
[
  {
    "id": 1,
    "name": "Стратегия и планирование",
    "createdAt": "2024-01-01T00:00:00"
  }
]
```

### Пользователи и вопросы

Пользователи и вопросы хранятся в памяти (in-memory) и теряются при перезапуске приложения.

## Структура проекта

```
src/main/java/com/bot/business_through_awareness/
├── bot/
│   ├── BusinessAwarenessBot.java      # Основной класс бота
│   └── BotInitializer.java            # Инициализация бота
├── config/
│   └── TelegramBotConfig.java         # Конфигурация бота
├── model/
│   ├── User.java                      # Модель пользователя
│   ├── Category.java                  # Модель категории
│   ├── Question.java                  # Модель вопроса
│   ├── UserState.java                 # Состояния пользователя
│   └── QuestionStatus.java            # Статусы вопроса
├── repository/
│   ├── UserRepository.java
│   ├── CategoryRepository.java
│   └── QuestionRepository.java
└── service/
    ├── UserService.java
    ├── CategoryService.java
    ├── QuestionService.java
    └── AnswerService.java
```

## Технологии

- Java 21
- Spring Boot 4.0.0
- Telegram Bot API (telegrambots-spring-boot-starter 6.9.7.1)
- Jackson (для работы с JSON)
- In-memory хранилище (для пользователей и вопросов)
- JSON файл (для категорий)

## Лицензия

Проект создан для внутреннего использования.

