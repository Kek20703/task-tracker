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

## Стек

Java 17, Spring Boot, Gradle, Postgres, Flyway, Kafka, Docker, GitHub Actions.

## Ветки

- `master` — стабильная ветка, из неё собираются Docker-образы.
- `dev` — интеграционная ветка, сюда вливаются PR с правками.
- `feature/*` — рабочие ветки под каждую правку.
