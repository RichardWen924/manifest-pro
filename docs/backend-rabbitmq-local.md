# Local Backend Infrastructure

This project uses RabbitMQ for async task processing, MinIO for local object storage, and Nacos for local service discovery and config center.

## Start

```bash
docker compose up -d rabbitmq minio minio-init nacos
```

## Stop

```bash
docker compose stop rabbitmq minio nacos
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

Nacos:

- Console URL: `http://127.0.0.1:8848/nacos`
- Server address: `127.0.0.1:8848`
- Auth: disabled in local compose by default
- Discovery group: `DEFAULT_GROUP`

## Service Discovery

`service-user`, `service-llm-task`, and `gateway` can register with Nacos in local development.

Use `NACOS_SERVER_ADDR=127.0.0.1:8848` when starting services outside Docker. For local JVM-to-JVM calls on the same machine, also set `NACOS_DISCOVERY_IP=127.0.0.1` so Nacos registers loopback addresses instead of a LAN address.

Leave `LLM_TASK_BASE_URL` empty to let `service-user` call `manifest-reader-llm-task` through Nacos discovery. Set `LLM_TASK_BASE_URL=http://127.0.0.1:18084` only when you want to bypass discovery for debugging.

Local Nacos auth is disabled, so leave `NACOS_USERNAME` and `NACOS_PASSWORD` empty. Only set them when you enable `NACOS_AUTH_ENABLE=true` on the server.

## Nacos Config Center

Each service imports profile-specific config from Nacos with:

```yaml
spring.config.import=optional:nacos:${spring.application.name}-${spring.profiles.active:dev}.yml
```

The local Nacos config DataIds are:

- `manifest-reader-gateway-dev.yml`
- `manifest-reader-user-dev.yml`
- `manifest-reader-llm-task-dev.yml`
- `manifest-reader-auth-dev.yml`
- `manifest-reader-admin-dev.yml`

Classpath `dev.yml` is still kept as a local fallback. If Nacos is unavailable, services can still start with the checked-in development config because the Nacos import is optional.

Republish one service config after editing local `dev.yml`:

```bash
curl -X POST 'http://127.0.0.1:8848/nacos/v1/cs/configs' \
  --data-urlencode 'dataId=manifest-reader-user-dev.yml' \
  --data-urlencode 'group=DEFAULT_GROUP' \
  --data-urlencode 'type=yaml' \
  --data-urlencode 'content@service/service-user/src/main/resources/dev.yml'
```

Fetch one service config from Nacos:

```bash
curl -s 'http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=manifest-reader-user-dev.yml&group=DEFAULT_GROUP'
```

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
docker compose ps rabbitmq minio nacos
curl -u guest:guest http://127.0.0.1:15672/api/overview
curl -f http://127.0.0.1:9000/minio/health/live
curl -f http://127.0.0.1:8848/nacos/actuator/health
```
