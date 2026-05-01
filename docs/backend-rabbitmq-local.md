# Local Backend Infrastructure

This project uses RabbitMQ for async task processing and MinIO for local object storage.

## Start

```bash
docker compose up -d rabbitmq minio minio-init
```

## Stop

```bash
docker compose stop rabbitmq minio
```

## Remove Container

```bash
docker compose down
```

## Management UI

RabbitMQ:

- URL: `http://127.0.0.1:15672`
- Username: `guest`
- Password: `guest`

MinIO:

- URL: `http://127.0.0.1:9001`
- Username: `minioadmin`
- Password: `minioadmin`
- Bucket: `manifest-reader`

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
docker compose ps rabbitmq minio
curl -u guest:guest http://127.0.0.1:15672/api/overview
curl -f http://127.0.0.1:9000/minio/health/live
```
