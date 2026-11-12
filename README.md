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

База — PostgreSQL 18. Если ранее инфраструктура поднималась на Postgres 17, старый volume
несовместим с новым образом: остановите контейнеры, удалите volume
(`docker volume rm task-tracker_postgres-data`) и запустите compose заново — миграции Liquibase
создадут схему с нуля, данные локальной разработки не переносятся.

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
./gradlew check      # тесты + проверка покрытия JaCoCo (не ниже 70% строк)
```

Отчёт о покрытии — `build/reports/jacoco/test/html/index.html`. Покрытие считается по всем
тестам из `test`, включая интеграционные; класс запуска приложения в подсчёт не входит.

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
./gradlew check     # тесты + проверка покрытия JaCoCo (не ниже 70% строк)
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

## Запуск summarization-server из исходников

Сервис читает запросы из `SUMMARIZATION_REQUESTS`, готовит отчёт через LLM и отвечает в
`SUMMARIZATION_RESPONSES`. Без `LLM_API_KEY` работает заглушка, поэтому цепочку можно проверить
локально без внешнего API.

Нужна отдельная база `summarization`. На свежем томе compose её создаёт init-скрипт. Если том уже
существовал, создайте один раз вручную:

```bash
docker exec -it task-tracker-postgres \
  psql -U task_tracker -d postgres -c "CREATE DATABASE summarization OWNER task_tracker;"
```

```bash
cd task-tracker-summarization-server
./gradlew bootRun   # слушает Kafka и отвечает в топик ответов
./gradlew test      # Kafka и Postgres в Testcontainers, нужен Docker
./gradlew check     # тесты + покрытие JaCoCo
```

Настройки: `KAFKA_BOOTSTRAP_SERVERS`, `POSTGRES_*`, `SUMMARIZATION_DB`,
`SUMMARIZATION_REQUESTS_TOPIC`, `SUMMARIZATION_RESPONSES_TOPIC`, `LLM_BASE_URL`, `LLM_API_KEY`,
`LLM_MODEL`, `LLM_TIMEOUT`. При ошибке или таймауте LLM сервис отвечает шаблонным отчётом.

## Запуск scheduler из исходников

Scheduler раз в сутки читает задачи из Postgres, запрашивает отчёт у summarization-сервиса и
публикует письмо в `EMAIL_SENDING_TASKS`. Нужны JDK 17, запущенные Postgres, Kafka и
summarization-сервис.

```bash
cd task-tracker-scheduler
./gradlew bootRun   # ждёт полночь по SCHEDULER_ZONE
./gradlew test      # Postgres и Kafka в Testcontainers, нужен Docker
./gradlew check     # тесты + покрытие JaCoCo
```

Для ручного запуска без ожидания полуночи:

```bash
SCHEDULER_RUN_ON_STARTUP=true ./gradlew bootRun
```

Настройки: `KAFKA_BOOTSTRAP_SERVERS`, `POSTGRES_*`, `POSTGRES_DB`, `SCHEDULER_ZONE`,
`SCHEDULER_CRON`, `SUMMARIZATION_TIMEOUT`, `SUMMARIZATION_REQUESTS_TOPIC`,
`SUMMARIZATION_RESPONSES_TOPIC`, `EMAIL_SENDING_TASKS_TOPIC`.

## Деплой в Docker

Полный стек описан в `docker-compose.prod.yml`: backend, email-sender, summarization, scheduler,
Postgres, Kafka и Mailpit. Backend публикуется на порту 80, поэтому приложение открывается по
`http://<server_ip>`.

```bash
cp .env.example .env   # заполнить POSTGRES_PASSWORD, JWT_SECRET, SMTP и LLM при необходимости
docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml up -d --build
```

Для существующего тома Postgres базу `summarization` нужно создать один раз вручную:

```bash
docker exec -it task-tracker-postgres \
  psql -U task_tracker -d postgres -c "CREATE DATABASE summarization OWNER task_tracker;"
```

На сервере без исходников используйте образы из Docker Hub: задайте `DOCKER_NAMESPACE` и
выполните `docker compose -f docker-compose.prod.yml pull` вместо `--build`. Для публикации
образов в GitHub Actions нужны секреты `DOCKERHUB_USERNAME` и `DOCKERHUB_TOKEN`.
Swagger на сервере по умолчанию выключен (`SWAGGER_ENABLED=false`).

## Стек

Java 17, Spring Boot, Gradle, Postgres, Liquibase, Kafka, Docker, GitHub Actions.

## План

Этапы разработки и их статус — в [docs/PLAN.md](docs/PLAN.md).

## Ветки

- `master` — стабильная ветка, из неё собираются Docker-образы.
- `dev` — интеграционная ветка, сюда вливаются PR с правками.
- `feature/*` — рабочие ветки под каждую правку.
