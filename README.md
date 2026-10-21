# LMS, лабораторная работа 3 (Java)

Maven multi-module: GraphQL-шлюз отдельно, внутренние сервисы — отдельные модули со своими базами PostgreSQL. Межсервисные вызовы — порты-заглушки (без gRPC и Kafka). Lombok используется для сущностей и сервисов.

## Модули

| Модуль | Роль |
|--------|------|
| `shared` | порты (будущий gRPC), DTO, общая JPA-сборка |
| `identity`, `catalog`, `enrollment`, `assessment`, `progress`, `notification`, `analytics` | внутренние сервисы, каждый со своей БД |
| `gateway` | GraphQL Query, единственное запускаемое приложение |

## Запуск

Нужны **JDK 17+**, **Docker Desktop**.

```
docker compose up -d
mvn -pl gateway -am spring-boot:run
```

Базы: `lms_identity`, `lms_catalog`, `lms_enrollment`, `lms_assessment`, `lms_progress`, `lms_notification`, `lms_analytics`. Пользователь `lms` / `lms`, порт `5433` (чтобы не пересечься с локальным PostgreSQL на 5432).

Таблицы в единственном числе, как на схеме ЛР2 (`user`, `course`, `assignment`, `course_stat` и т.д.). Если PostgreSQL уже поднимали со старой схемой, пересоздайте тома: `docker compose down -v && docker compose up -d`.

- GraphQL: http://127.0.0.1:8080/graphql
- GraphiQL: http://127.0.0.1:8080/graphiql

Заголовок `X-User-Id` задаёт текущего пользователя. Если его нет, используется тестовый студент
`11111111-1111-1111-1111-111111111111`.

Идентификатор курса: `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`.

## Проверка

```
mvn test
```
