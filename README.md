# Hotel Booking System

## Описание

Проект представляет собой разработку REST API для системы бронирования отелей на базе Spring Boot с использованием микросервисной архитектуры.

## Технологии

- Java 17.
- Spring Boot 3.5.7.
- Spring Cloud 2025.0.0.
- Spring Data JPA.
- Spring Security + JWT.
- Spring Cloud Eureka (Service Discovery).
- Spring Cloud Gateway (API Gateway).
- Lombok, MapStruct (для DTO и маппинга).

## Компоненты системы

1. API Gateway (Spring Cloud Gateway) — шлюз, осуществляющий маршрутизацию запросов, передачу токена для проверки в сервисах.

2. Hotel Management Service — микросервис для управления отелями и номерами (CRUD), агрегации по загруженности.

3. Booking Service — микросервис для создания бронирований и управления ими, интеграции с Hotel Service, регистрации и авторизации пользователей, администрирования.

4. Eureka Server — реализация Service Registry (реестр сервисов). Его основная задача — обеспечивать динамическое обнаружение сервисов (service discovery) в микросервисной архитектуре.

# Как пользоваться системой

## Клонируйте репозиторий:

```bash
git clone https://github.com/nikolay-977/hotel-booking-system
cd hotel-booking-system
 ```

## Запустите Hotel Booking System

### 1. Eureka Server
```
mvn spring-boot:run -pl eureka-server
```

### 2. Hotel Service
```
mvn spring-boot:run -pl hotel-service
```

### 3. Booking Service
```
mvn spring-boot:run -pl booking-service
```

### 4. API Gateway
```
mvn spring-boot:run -pl api-gateway
```

## Эндпоинты

### Gateway (маршрутизация)
/api/bookings/** → Booking Service.
/api/hotels/** → Hotel Service.

### Booking Service
- DELETE /user — удалить пользователя (ADMIN).
- POST /user — создать пользователя (ADMIN).
- PATCH /user — обновить данные пользователя (ADMIN).
- POST /booking — создать бронирование (с выбором или автоподбором комнаты) (USER). В теле запроса параметр autoSelect: true/false (при true поле roomId игнорируется).
- GET /bookings — история бронирований пользователя (USER).
- POST /user/register — зарегистрировать пользователя, сгенерировав токен (USER).
- POST /user/auth — авторизовать пользователя, сгенерировав токен (USER).
- GET /booking/{id} — получить бронирование по id (USER).
- DELETE /booking/{id} — отменить бронирование (USER).

### Hotel Management Service
- POST /api/hotels — добавить отель (ADMIN).
- POST /api/rooms — добавить номер в отель (ADMIN).
- GET /api/hotels — получить список отелей (USER).
- GET /api/rooms/recommend — получить список рекомендованных номеров (USER) (те же свободные номера, отсортированные по возрастанию times_booked).
- GET /api/rooms — получить список всех свободных номеров (USER) (без специальной сортировки).
- POST /api/rooms/{id}/confirm-availability — подтвердить доступность номера на запрошенные даты (временная блокировка слота на указанный период, используется в шаге согласованности) (INTERNAL).
- POST /api/rooms/{id}/release — компенсирующее действие: снять временную блокировку слота (INTERNAL). Маршрут не публикуется через Gateway.

# Как протестировать. Postman env и коллекции для тестирования:

- [Hotel Booking Environment.postman_environment.json](postman/Hotel%20Booking%20Environment.postman_environment.json)
- [Hotel Booking System API.postman_collection.json](postman/Hotel%20Booking%20System%20API.postman_collection.json)

# Swagger

## 1. Booking-service
http://localhost:8080/booking-swagger/swagger-ui/index.html

## 2. Hotel-service
http://localhost:8080/hotel-swagger/swagger-ui/index.html
