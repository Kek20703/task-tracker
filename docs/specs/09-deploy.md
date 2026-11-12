# Этап 9. Docker-образы, CI/CD, деплой

> Статус постановки: `утверждена`. Дата утверждения: 2026-11-12. Ветка: `feature/deploy`.
> После утверждения изменения — только отдельным решением.

## Цель

Собирать все четыре сервиса в Docker-образы, гонять проверки на PR, публиковать образы из
`master` и запускать полный стек на сервере.

## Границы

Входит:

- Dockerfile на каждый сервис, двухстадийный: JDK-сборка, JRE-запуск;
- `docker-compose.prod.yml`: backend, email-sender, summarization, scheduler, Postgres, Kafka, Mailpit;
- CI на PR: `./gradlew check` для всех четырёх сервисов;
- CD при пуше в `master`: сборка и публикация образов в Docker Hub;
- инструкция деплоя и переменные окружения.

Не входит:

- реальная публикация образов и деплой без доступов владельца;
- HTTPS и домен — по ТЗ достаточно `http://$server_ip`;
- мониторинг, бэкапы, оркестратор кроме compose.

## Docker

- Образы: `task-tracker-backend`, `-email-sender`, `-summarization-server`, `-scheduler`.
- Backend публикуется на `${BACKEND_PORT:-80}:8080`; остальные сервисы портов не открывают.
- Внутри compose адреса: `postgres:5432`, `kafka:9092`, `mailpit:1025`.
- `SPRING_DATASOURCE_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `MAIL_*`, `LLM_*` задаются окружением.

## CI

- GitHub Actions на `pull_request`.
- Матрица из четырёх сервисов, JDK 17, `./gradlew check`.
- Docker нужен для Testcontainers; на ubuntu-runner он есть.
- Кэш Gradle через `gradle/actions/setup-gradle`.

## CD

- При пуше в `master` и вручную: сборка образов и push в Docker Hub.
- Секреты GitHub: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`.
- Теги: `latest` и `sha-<commit>`.
- Без секретов workflow не публикует, но не ломает CI.

## Деплой

```bash
cp .env.example .env   # заполнить секреты
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

- Backend доступен по `http://$server_ip` (порт 80).
- На существующем томе Postgres базу `summarization` создать один раз вручную.
- Swagger на сервере выключается `SWAGGER_ENABLED=false`.

## Нужно от владельца

- аккаунт Docker Hub и токен;
- SMTP Unisender: host, port, login, password, from;
- Linux-сервер с Docker и compose.

## Критерии готовности

- [x] Dockerfile собирает каждый сервис.
- [x] `docker-compose.prod.yml` проходит `docker compose config`.
- [x] CI workflow проверяет все четыре сервиса.
- [x] CD workflow публикует образы при наличии секретов.
- [x] README описывает деплой и обязательные переменные.
- [x] Для всех сервисов `./gradlew check` остаётся зелёным.

## Проверки

- [x] `docker compose -f docker-compose.prod.yml config`.
- [x] Сборка хотя бы одного образа локально.
- [x] `./gradlew check` во всех сервисах.
