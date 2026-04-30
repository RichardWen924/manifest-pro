# Local RabbitMQ

This project uses RabbitMQ as the local message broker for async task processing.

## Start

```bash
docker compose up -d rabbitmq
```

## Stop

```bash
docker compose stop rabbitmq
```

## Remove Container

```bash
docker compose down
```

## Management UI

- URL: `http://127.0.0.1:15672`
- Username: `guest`
- Password: `guest`

## service-user with MQ Consumers Enabled

`service-user` keeps RabbitMQ listeners disabled by default in `dev` so local development can still start without MQ.

When RabbitMQ is running, enable listeners and the Rabbit health indicator like this:

```bash
RABBITMQ_LISTENER_AUTO_STARTUP=true \
BILL_PARSE_LISTENER_ENABLED=true \
RABBITMQ_HEALTH_ENABLED=true \
java -jar service/service-user/target/service-user-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## Quick Verification

```bash
docker compose ps rabbitmq
curl -u guest:guest http://127.0.0.1:15672/api/overview
```
