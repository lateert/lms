# LMS

Maven multi-module: GraphQL-шлюз отдельно, внутренние сервисы — отдельные модули со своими базами PostgreSQL. Межсервисные вызовы — порты-заглушки gRPC, события — заглушка шины (Spring `ApplicationEventPublisher` вместо Kafka). Lombok используется для сущностей и сервисов.

## Модули


| Модуль                                                                                     | Роль                                                                                   |
| ------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------- |
| `shared`                                                                                   | порты (будущий gRPC), события, input-типы, DTO, `AppException`, общая JPA-сборка       |
| `identity`, `catalog`, `enrollment`, `assessment`, `progress`, `notification`, `analytics` | внутренние сервисы: бизнес-операции, проверки, обработчики событий; каждый со своей БД |
| `gateway`                                                                                  | GraphQL Query и Mutation, заглушка шины событий, единственное запускаемое приложение   |


## Запуск

Нужны **JDK 17+**, **Docker Desktop**.

```
docker compose up -d
mvn -pl gateway -am spring-boot:run
```

Базы: `lms_identity`, `lms_catalog`, `lms_enrollment`, `lms_assessment`, `lms_progress`, `lms_notification`, `lms_analytics`. Пользователь `lms` / `lms`, порт `5433` (чтобы не пересечься с локальным PostgreSQL на 5432).

Таблицы в единственном числе, как на схеме ЛР2. В ЛР4 добавлены `completed_lesson` (Progress) и колонка `course_stat.grade_count`, пароли хранятся bcrypt-хешем. Если PostgreSQL поднимали со схемой ЛР3, пересоздайте тома: `docker compose down -v && docker compose up -d`.

- GraphQL: [http://127.0.0.1:8080/graphql](http://127.0.0.1:8080/graphql)
- GraphiQL: [http://127.0.0.1:8080/graphiql](http://127.0.0.1:8080/graphiql) (параметры `?query=...&headers=...` подставляют запрос и заголовки)

## Пользователь запроса

1. `Authorization: Bearer <token>` — токен из Mutation `login` или `register`.
2. `X-User-Id: <id>` — для отладки.
3. Без заголовков — тестовый студент `11111111-1111-1111-1111-111111111111`.

Демо-пользователи (пароль `password`): `student@lms.local`, `teacher@lms.local`, `admin@lms.local`.

```graphql
mutation {
  login(input: {email: "teacher@lms.local", password: "password"}) { token }
}
```

Ошибки бизнес-операций приходят в `errors[].extensions.code`: `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`.

## Проверка

```
mvn test
```

`GraphqlQueryTest` — чтение, `GraphqlMutationTest` — сквозной сценарий (курс → публикация → запись → урок → сдача → оценка → уведомления, витрина, рейтинг), фильтры и сводки каталога, назначение роли.