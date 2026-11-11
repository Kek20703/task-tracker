# Task Tracker

Многопользовательский планировщик задач — учебный проект по
[ТЗ из Java-роадмапа Сергея Жукова](https://zhukovsd.github.io/java-backend-learning-course/projects/task-tracker/).

## Сервисы

| Сервис | Назначение |
|--------|------------|
| `task-tracker-backend` | REST API: пользователи, JWT-авторизация, задачи. Порт 8080 |
| `task-tracker-email-sender` | Читает топик `EMAIL_SENDING_TASKS` и отправляет письма |
| `task-tracker-summarization-server` | Генерирует отчёт по задачам через LLM (Kafka RPC) |
| `task-tracker-scheduler` | Раз в сутки собирает отчёты и ставит письма в очередь |

Инфраструктура: Postgres, Kafka. Всё запускается через Docker Compose.

## Локальный запуск инфраструктуры

```bash
cp .env.example .env   # и задать свои POSTGRES_PASSWORD и JWT_SECRET
docker compose up -d
```

| Что | Адрес с хоста | Адрес внутри compose-сети |
|-----|---------------|---------------------------|
| Postgres | `localhost:5432` | `postgres:5432` |
| Kafka | `localhost:29092` | `kafka:9092` |
| SMTP (Mailpit) | `localhost:1025` | `mailpit:1025` |
| Пойманные письма | http://localhost:8025 | — |

## Запуск backend из исходников

Нужны JDK 17 и поднятая инфраструктура (см. выше). Пароль базы сервис берёт из переменной
окружения `POSTGRES_PASSWORD` — той же, что в `.env`. Оттуда же берётся `JWT_SECRET`.

```bash
cd task-tracker-backend
./gradlew bootRun    # сервис на http://localhost:8080
./gradlew test       # тесты; для интеграционных нужен запущенный Docker
```

При запуске из IntelliJ IDEA переменную нужно задать в конфигурации запуска:
Run → Edit Configurations → Environment variables → `POSTGRES_PASSWORD=<пароль из .env>`.
Без неё сервис не стартует с ошибкой `password authentication failed`.

Секрет подписи токенов сервис берёт из `JWT_SECRET` (не короче 32 символов) — его нужно задать так же.

Примеры запросов — в `task-tracker-backend/http/*.http` (REST Client в IntelliJ IDEA).

### Swagger UI

После запуска backend документация API с возможностью отправлять запросы открывается на
http://localhost:8080/swagger-ui.html (описание в формате OpenAPI — http://localhost:8080/v3/api-docs).

1. `POST /user` или `POST /auth/login` → **Try it out** → **Execute**.
2. Скопировать токен из заголовка ответа `authorization` — без слова `Bearer`.
3. Кнопка **Authorize** вверху страницы → вставить токен → **Authorize**. После этого работают
   методы с замком: `GET /user` и всё про задачи.

## Запуск email-sender из исходников

Нужны JDK 17 и та же инфраструктура. Сервис читает задания на отправку из Kafka и шлёт письма
через SMTP. Значения по умолчанию рассчитаны на запуск из IDE рядом с compose: брокер —
`localhost:29092`, почта — Mailpit на `localhost:1025`.

```bash
cd task-tracker-email-sender
./gradlew bootRun   # подключается к Kafka и слушает топик EMAIL_SENDING_TASKS
./gradlew test      # тесты; для интеграционных (Kafka в Testcontainers) нужен запущенный Docker
```

Пойманные письма видны в Mailpit: http://localhost:8025. Проверить вручную можно так — положить
письмо в топик и убедиться, что оно появилось в Mailpit:

```bash
docker exec -i task-tracker-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic EMAIL_SENDING_TASKS
# ввести строку: {"to":"user@example.com","subject":"Тема","text":"Текст"}
```

Настройки переопределяются переменными окружения: `KAFKA_BOOTSTRAP_SERVERS`, `MAIL_HOST`,
`MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`,
`MAIL_SMTP_STARTTLS`. При сбое отправки сервис делает до трёх попыток, после чего перекладывает
письмо в топик `EMAIL_SENDING_TASKS.DLT` (число попыток и пауза — `EMAIL_RETRY_MAX_ATTEMPTS`,
`EMAIL_RETRY_INTERVAL_MS`).

## Стек

Java 17, Spring Boot, Gradle, Postgres, Liquibase, Kafka, Docker, GitHub Actions.

## План

Этапы разработки и их статус — в [docs/PLAN.md](docs/PLAN.md).

## Ветки

- `master` — стабильная ветка, из неё собираются Docker-образы.
- `dev` — интеграционная ветка, сюда вливаются PR с правками.
- `feature/*` — рабочие ветки под каждую правку.
