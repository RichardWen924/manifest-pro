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

`gateway`, `service-auth`, `service-admin`, `service-user`, and `service-llm-task` can register with Nacos in local development.

Use `NACOS_SERVER_ADDR=127.0.0.1:8848` when starting services outside Docker. For local JVM-to-JVM calls on the same machine, also set `NACOS_DISCOVERY_IP=127.0.0.1` so Nacos registers loopback addresses instead of a LAN address.

Leave `LLM_TASK_BASE_URL` empty to let `service-user` call `manifest-reader-llm-task` through Nacos discovery. Set `LLM_TASK_BASE_URL=http://127.0.0.1:18084` only when you want to bypass discovery for debugging.

Local Nacos auth is disabled, so leave `NACOS_USERNAME` and `NACOS_PASSWORD` empty. Only set them when you enable `NACOS_AUTH_ENABLE=true` on the server.

Service names used by Feign and gateway routes:

- Gateway routes: `lb://manifest-reader-auth`, `lb://manifest-reader-admin`, `lb://manifest-reader-user`
- Admin -> Auth: `manifest-reader-auth`
- Admin -> User: `manifest-reader-user`
- User -> Auth: `manifest-reader-auth`
- User -> LLM Task: `manifest-reader-llm-task`
- Gateway -> Market: `manifest-reader-market`

## service-market

- Service name: `manifest-reader-market`
- Default local port: `8085`
- Gateway route: `/market/**`
- Async follow-up queue: `freight.demand.accepted.queue`
- Async follow-up exchange: `freight.demand.accepted.exchange`
- Async follow-up routing key: `freight.demand.accepted.route`

`service-market` keeps RabbitMQ listener startup configurable in `dev`:

- `RABBITMQ_LISTENER_AUTO_STARTUP=false`: 接单会发 MQ，但消息保留在队列里等待消费
- `RABBITMQ_LISTENER_AUTO_STARTUP=true`: 本地消费者会消费接单事件并写入 `freight_order_timeline`

Recommended local startup:

```bash
NACOS_SERVER_ADDR=127.0.0.1:8848 \
NACOS_DISCOVERY_IP=127.0.0.1 \
RABBITMQ_LISTENER_AUTO_STARTUP=true \
RABBITMQ_HEALTH_ENABLED=true \
java -jar service/service-market/target/service-market-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.port=18086 --server.address=127.0.0.1
```

If `18085` is already occupied by the frontend dev server, use `18086` or another dedicated backend port for backend smoke tests.

High-cost LLM work should still be asynchronous. Feign is used for control-plane calls such as task submission, status query, identity lookup, and admin aggregation. RabbitMQ remains responsible for queueing extract/export/save workloads so traffic spikes do not overload the LLM workflow.

Marketplace quote acceptance also follows this split:

- Redis uses `market:demand:accept:{demandId}` as a short lock to prevent duplicate acceptance
- synchronous transaction creates the order and locks the demand
- RabbitMQ publishes an `ORDER_CREATED` follow-up event
- consumer writes `freight_order_timeline`

If Feign calls are flaky, first check whether Nacos still has stale local instances:

```bash
curl -s 'http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=manifest-reader-user&groupName=DEFAULT_GROUP'
```

Stop old JVM processes or restart them with `NACOS_DISCOVERY_IP=127.0.0.1` until only reachable loopback instances remain.

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
curl -s 'http://127.0.0.1:8848/nacos/v1/ns/catalog/services?hasIpCount=true&withInstances=false&pageNo=1&pageSize=20'
curl -s -i 'http://127.0.0.1:18081/admin/users/u-1001/bills'
curl -s http://127.0.0.1:18086/actuator/health
curl -s -X POST http://127.0.0.1:18086/market/demands -H 'Content-Type: application/json' -H 'X-Company-Id: 2' -H 'X-User-Id: 3' -d '{"title":"上海到汉堡拼箱","goodsName":"家具","departurePort":"SHANGHAI","destinationPort":"HAMBURG","quantity":6,"quantityUnit":"CBM","budgetAmount":3200,"currencyCode":"CNY","attachmentFileIds":[]}'
curl -s -u guest:guest 'http://127.0.0.1:15672/api/queues/%2F/freight.demand.accepted.queue'
```
