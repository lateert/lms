# LMS

Maven multi-module: GraphQL-шлюз и внутренние сервисы со своими базами PostgreSQL. Синхронные проверки между сервисами идут по gRPC (protobuf, отдельные порты). События публикует Kafka producer и разбирает consumer. Lombok используется для сущностей и сервисов.

## Модули


| Модуль                                                                                     | Роль                                                                                   |
| ------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------- |
| `shared`                                                                                   | protobuf-контракты, gRPC-клиенты, события, input-типы, DTO, `AppException`, общая JPA-сборка |
| `identity`, `catalog`, `enrollment`, `assessment`, `progress`, `notification`, `analytics` | внутренние сервисы: бизнес-операции, проверки, gRPC-серверы Identity, Catalog и Enrollment |
| `gateway`                                                                                  | GraphQL Query и Mutation, Kafka producer/consumer, единственное запускаемое приложение |


## Запуск

Нужны **JDK 17+**, **Docker Desktop**.

```
docker compose up -d
mvn -pl gateway -am spring-boot:run
```

Базы: `lms_identity`, `lms_catalog`, `lms_enrollment`, `lms_assessment`, `lms_progress`, `lms_notification`, `lms_analytics`. Пользователь `lms` / `lms`, порт `5433` (чтобы не пересечься с локальным PostgreSQL на 5432). Redis для сессий — `localhost:6379`. Kafka — `localhost:9092`. Оба контейнера в том же `docker compose`.

gRPC: Identity `localhost:9101`, Catalog `localhost:9102`, Enrollment `localhost:9103`. Шлюз проверяет сессию вызовом `ValidateSession`. Enrollment перед записью вызывает `GetPublishedCourse`, Assessment — `LessonExists` и `IsEnrolled`, Progress — `GetCourseCounts`. Если сервис не отвечает, клиент получает код `UNAVAILABLE`. События (`UserRegistered`, `CoursePublished`, `StudentEnrolled`, `LessonCompleted`, `AssignmentSubmitted`, `GradeAssigned`, `CourseRatingUpdated`) пишутся в топик `lms.events` после фиксации транзакции. Consumer создаёт уведомления, прогресс и витрину аналитики.

Таблицы в единственном числе, как на схеме ЛР2. В ЛР4 добавлены `completed_lesson` (Progress) и колонка `course_stat.grade_count`, пароли хранятся bcrypt-хешем. Если PostgreSQL поднимали со схемой ЛР3, пересоздайте тома: `docker compose down -v && docker compose up -d`.

- GraphQL: [http://127.0.0.1:8080/graphql](http://127.0.0.1:8080/graphql)
- GraphiQL: [http://127.0.0.1:8080/graphiql](http://127.0.0.1:8080/graphiql) (параметры `?query=...&headers=...` подставляют запрос и заголовки)

## Пользователь запроса

`Authorization: Bearer <jwt>` — токен из Mutation `login` или `register`. Identity проверяет подпись JWT и активную сессию в Redis (TTL 2 часа). После `logout` или истечения TTL тот же токен отклоняется. Каталог (`catalog`, `catalogSummary`, `course`) доступен без входа. Личные Query и все остальные Mutation требуют сессию. Роли: студент записывается и сдаёт работы, преподаватель ведёт курс и ставит оценки, администратор назначает роли.

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

`GraphqlQueryTest` — чтение, `GraphqlMutationTest` — сквозной сценарий через gRPC и Kafka, `GraphqlSecurityTest` — JWT, отзыв сессии в Redis и запрет операции по роли. Нужны запущенные PostgreSQL, Redis и Kafka.