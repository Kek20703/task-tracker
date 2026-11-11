# Техдолг. UUIDv7 как первичный ключ, PostgreSQL 18 и Spring Data JDBC

> Статус постановки: `утверждена`. Дата утверждения: 2026-11-11. Ветка: `feature/uuid-pk`.
> После утверждения изменения — только отдельным решением.
> Работа делится на два PR: сначала UUIDv7 и PostgreSQL 18, затем Spring Data JDBC. Оба — до этапа 7.

## Цель

Первичные ключи пользователей и задач — UUIDv7 вместо последовательных `bigint`: последовательные
id раскрывают количество пользователей и задач. База — PostgreSQL 18 (нужна для `uuidv7()`).
Доступ к данным — Spring Data JDBC вместо Spring Data JPA/Hibernate: без прокси-сущностей,
ленивых загрузок и контекста персистентности.

## Границы

Входит:

- **PR 1 (UUID + PostgreSQL 18):**
  - образы `postgres:17-alpine` → `postgres:18-alpine` в `docker-compose.yml` и `TestcontainersConfiguration`;
  - зависимость `com.github.f4b6a3:uuid-creator` в backend;
  - миграция Liquibase `003`: `users.id`, `tasks.id`, `tasks.user_id` → `uuid` с backfill через `uuidv7()`;
  - генерация UUIDv7 в приложении при вставке;
  - замена `Long` → `UUID` в сущностях, DTO, JWT, сервисах, контроллерах, тестах и `.http`-коллекции.
- **PR 2 (Spring Data JDBC), отдельная ветка после слияния PR 1:**
  - `spring-boot-starter-data-jpa` → `spring-boot-starter-data-jdbc` (и тестовый starter);
  - сущности на `@Table`/`@Column` Spring Data Relational, `Persistable<UUID>` для детекции новой записи;
  - `CrudRepository` вместо `JpaRepository`;
  - блокировка задачи `@Query("... FOR UPDATE")` вместо `@Lock(PESSIMISTIC_WRITE)`;
  - `created_at` через `@CreatedDate` + JDBC-аудит (или установку в коде);
  - разбор `DataIntegrityViolationException` без классов Hibernate;
  - `ArchitectureTest` перевести с JPA-аннотаций на `@Table`, не ослабляя правила.

Не входит:

- формат JWT, кроме типа `sub` (подпись, TTL, заголовки не меняются);
- контракты Kafka и email-sender — `EmailMessage` без изменений;
- контракты API, кроме типа id в ответах и путях;
- scheduler и summarization (этапы 7–8) — они получат UUID-схему сразу в своей постановке;
- оптимизации индексов сверх переноса существующих.

## Контракты

- **API:** пути, методы и коды ответов не меняются. `id` пользователя и задачи в ответах —
  строка UUID. `GET/PATCH/DELETE /tasks/{id}` принимают UUID; не-UUID в пути — `400`.
  Сортировка списка задач по `id` сохраняется — UUIDv7 монотонны по времени создания.
- **JWT:** `sub` — строка UUID пользователя вместо числа. Токены, выпущенные до слияния PR 1,
  перестают действовать (subject больше не парсится как UUID) — нужен повторный вход;
  при TTL 24 ч это приемлемо.
- **DTO:** `UserResponse(UUID id, String email)`, `TaskResponse(UUID id, ...)`,
  `CurrentUser(UUID id, String email)`. `EmailMessage` не меняется.
- **Ошибки:** дубликат email — `409`, чужая или несуществующая задача — `404`, невалидные данные — `400`,
  внутренняя ошибка — `500` с текстом `Internal server error`. Поведение при сбоях — см. ниже.

## Данные и миграции

- **PostgreSQL 18.** Локальный volume от PG 17 несовместим с PG 18: при обновлении volume
  пересоздаётся, данные локальной разработки не переносятся; случай описать в README.
  Прод-базы ещё нет (этап 9 не сделан), мигрировать на сервере нечего.
- **Миграция `003-uuid-pk.xml`** (после 002):
  1. `users`: добавить колонку `id_uuid uuid`, заполнить `uuidv7()`;
  2. `tasks`: добавить `id_uuid uuid` и `user_id_uuid uuid`; `id_uuid` — `uuidv7()`,
     `user_id_uuid` — по join с `users`;
  3. удалить старые PK (`pk_users`, `pk_tasks`), FK `fk_tasks_user`, колонки `bigint`
     (их sequence удалятся вместе с колонками);
  4. переименовать `id_uuid` → `id`, `user_id_uuid` → `user_id`;
  5. восстановить `pk_users`, `pk_tasks`, `fk_tasks_user` (ON DELETE CASCADE), `ix_tasks_user_id`;
  6. уникальный индекс `uq_users_email_lower` и check-констрейнты `tasks` не затрагиваются.
- Колонки `id` — `uuid` без autoIncrement с `default uuidv7()`: новые id генерирует приложение
  (uuid-creator), дефолт в базе — страховка для записей вне приложения.
- `status` в `tasks` хранится строкой `TODO`/`DONE`; в Spring Data JDBC проверить маппинг enum
  по имени (при необходимости — `@Enum` или конвертер).
- После PR 1 `ddl-auto: validate` проходит: сущность `UUID` ↔ колонка `uuid`.

## Конфигурация

- Новых переменных окружения нет.
- `build.gradle` backend: `implementation 'com.github.f4b6a3:uuid-creator:6.1.1'` (PR 1);
  замена data-jpa starter на data-jdbc (PR 2).
- `application.yml`: в PR 2 убрать блок `spring.jpa`.

## Поведение при сбоях

- **Гонка «создание задачи vs удаление пользователя»:** сейчас `DataIntegrityViolationException`
  разбирается через Hibernate `ConstraintViolationException` по имени FK `fk_tasks_user` →
  `AccountNotFoundException`. В PR 2 то же поведение без классов Hibernate: имя констрейнта
  достаётся из cause-цепочки (`PSQLException`/`SQLException`).
- **Блокировка задач:** `SELECT ... FOR UPDATE` в `@Query` даёт то же поведение, что `@Lock`:
  параллельные автосохранения выполняются по очереди, `TaskConcurrencyIT` остаётся зелёным.
- **Недоступная БД:** как сейчас — `500 Internal server error`, детали в логе.
- **Старые JWT:** становятся невалидны после PR 1 — ожидаемо, см. «Контракты».

## Критерии готовности

PR 1:

- [ ] `docker-compose.yml` и Testcontainers используют `postgres:18-alpine`.
- [ ] Миграция `003` переносит данные: старые `bigint`-колонки удалены, PK/FK/индексы восстановлены.
- [ ] API принимает и возвращает UUID; JWT `sub` — UUID; `.http`-коллекция обновлена.
- [ ] `./gradlew check` зелёный, покрытие не ниже 70%.

PR 2:

- [ ] JPA/Hibernate удалён из зависимостей backend; сущности — `@Table` Spring Data Relational,
  репозитории — `CrudRepository`.
- [ ] Блокировка задач — `@Query ... FOR UPDATE`; тест параллельных автосохранений зелёный.
- [ ] `created_at` заполняется при вставке (`@CreatedDate`/аудит или в коде).
- [ ] Разбор ошибок целостности без классов Hibernate; регистрация существующего email — `409`.
- [ ] `ArchitectureTest` проверяет `@Table` вместо JPA-аннотаций; правила не ослаблены.
- [ ] `./gradlew check` зелёный, покрытие не ниже 70%.

## Проверки

- [ ] `./gradlew check` в `task-tracker-backend` после каждого PR.
- [ ] Ручной прогон: поднять compose, применить миграцию `003` на базе с данными, пройти
      регистрацию и операции с задачами через `.http`, убедиться, что id — UUID.
- [ ] Проверить, что запрос со старым токеном даёт `401`, а повторный вход — новый токен.

## Открытые вопросы

Нет.
