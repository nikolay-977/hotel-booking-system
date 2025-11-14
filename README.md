# Клонируйте репозиторий:

```bash
git clone https://github.com/nikolay-977/hotel-booking-system
cd hotel-booking-system
 ```

# Запустите Hotel Booking System

## 1. Eureka Server
```
mvn spring-boot:run -pl eureka-server
```

## 2. Hotel Service
```
mvn spring-boot:run -pl hotel-service
```

## 3. Booking Service
```
mvn spring-boot:run -pl booking-service
```

## 4. API Gateway
```
mvn spring-boot:run -pl api-gateway
```

# Postman-коллекции для тестирования

[Hotel Booking Environment.postman_environment.json](postman/Hotel%20Booking%20Environment.postman_environment.json)
[Hotel Booking System API.postman_collection.json](postman/Hotel%20Booking%20System%20API.postman_collection.json)

# Swagger

## booking-service
http://localhost:8080/booking-swagger/swagger-ui/index.html

## hotel-service
http://localhost:8080/hotel-swagger/swagger-ui/index.html