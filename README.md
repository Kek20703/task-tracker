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
cp .env.example .env   # и задать свой POSTGRES_PASSWORD
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
окружения `POSTGRES_PASSWORD` — той же, что в `.env`.

```bash
cd task-tracker-backend
./gradlew bootRun    # сервис на http://localhost:8080
./gradlew test       # тесты; для интеграционных нужен запущенный Docker
```

При запуске из IntelliJ IDEA переменную нужно задать в конфигурации запуска:
Run → Edit Configurations → Environment variables → `POSTGRES_PASSWORD=<пароль из .env>`.
Без неё сервис не стартует с ошибкой `password authentication failed`.

Примеры запросов — в `task-tracker-backend/http/*.http` (REST Client в IntelliJ IDEA).

## Стек

Java 17, Spring Boot, Gradle, Postgres, Liquibase, Kafka, Docker, GitHub Actions.

## Ветки

- `master` — стабильная ветка, из неё собираются Docker-образы.
- `dev` — интеграционная ветка, сюда вливаются PR с правками.
- `feature/*` — рабочие ветки под каждую правку.
