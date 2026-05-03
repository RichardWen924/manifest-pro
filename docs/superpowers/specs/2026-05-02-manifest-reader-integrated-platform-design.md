# Manifest Reader Integrated Platform Design

## Goal

Upgrade `manifestReader` from a document-processing system into an integrated shipping platform that combines:

- bill of lading extraction, template extraction, template save, and export
- a freight demand marketplace
- an industry news community
- unified frontend navigation and platform homepage

The result should feel like one coherent product instead of three disconnected demos.

## Current Execution Note

The long-term integrated-platform direction remains valid, but the active delivery scope has been narrowed:

- do now: freight marketplace
- do later: news community
- do later: unified platform homepage and navigation

For the current execution cycle, treat this document as the long-range blueprint and use the marketplace plan as the only active implementation target.

## Product Positioning

This platform is not a generic e-commerce mall and not a generic news app.

It should be positioned as:

`AI-powered shipping collaboration platform`

Core user value:

- shippers publish freight demands and attach shipping-related files
- agents or other customers bid, accept, and fulfill shipping work
- users continue using bill extraction, template generation, and export as platform productivity tools
- users browse shipping and trade news, then comment, like, collect, and discuss market changes

## Reference Mapping

The project should borrow patterns from three Heima projects, but translate them into the shipping domain instead of copying business models directly.

### Hmall Reference

Use as reference for:

- trading workflow
- order-like lifecycle design
- search entry points
- microservice boundaries
- RabbitMQ-based async decoupling

Translate to current project as:

- goods -> freight demand
- order -> transport agency order / fulfillment order
- seller operations -> demand publisher operations
- buyer operations -> agent / carrier operations

### Leadnews Reference

Use as reference for:

- content stream
- article detail and engagement
- comment and like interaction
- microservice governance with gateway and Nacos

Translate to current project as:

- news -> shipping industry article
- user feed -> shipping news stream
- article interaction -> comment / like / collect / share

### HMDP Reference

Use as reference for:

- Redis-first interaction design
- hot data caching
- like state modeling
- feed and ranking ideas
- high-concurrency request protection

Translate to current project as:

- flash sale patterns -> hot freight demand acceptance
- shop/blog engagement -> demand heat, news likes, ranking, and activity streams

## Priority Decision

Implementation priority should be:

1. freight marketplace
2. news community
3. unified frontend platform reconstruction

### Why This Order

The freight marketplace is the closest extension of the current shipping domain and can directly reuse:

- object storage
- async task center
- Nacos service governance
- RabbitMQ
- user and auth foundation

It also gives the strongest interview story because it combines domain fit, state transitions, and high-concurrency design.

The news community should come second because it is valuable, but it is still a supporting product layer rather than the core shipping workflow.

The unified frontend should come after the first two backend slices have real APIs and real states. This avoids building a polished shell around mock functionality.

## Service Architecture

### Existing Services Kept

- `gateway`: unified ingress, route aggregation
- `service-auth`: authentication, identity, token validation
- `service-user`: user-facing shipping tools and orchestration
- `service-admin`: admin management and operational views
- `service-llm-task`: heavy async processing for extraction, export, save

### New Services

- `service-market`: freight demand marketplace
- `service-news`: industry news and community interaction

### Responsibility Split

#### service-market

Owns:

- freight demand creation and listing
- demand detail
- bid / quote submission
- demand acceptance
- fulfillment order creation
- demand status flow
- hot demand caching and acceptance protection

It should remain the owner of marketplace domain data, but not the owner of management UI workflows.

#### service-admin

Owns marketplace management workflows:

- marketplace dashboard and statistics
- demand review / manual intervention
- fulfillment supervision
- operational status changes initiated by admins
- management-side search and filtering views

`service-admin` should call `service-market` for marketplace domain mutations and queries instead of duplicating marketplace state locally.

#### service-news

Owns:

- third-party news pulling
- article persistence
- article detail
- comment
- like
- collect
- hot ranking
- article search sync

#### service-user

Continues to own:

- bill extraction entry
- template extraction entry
- template save entry
- template export entry
- file download coordination
- platform user homepage aggregation where needed

`service-user` should not become the owner of marketplace or news core data, and it should not absorb marketplace management responsibilities.

## Domain Model

### Marketplace

Main entities:

- `freight_demand`
- `freight_demand_attachment`
- `freight_quote`
- `freight_order`
- `freight_order_timeline`

Suggested demand states:

- `DRAFT`
- `PUBLISHED`
- `QUOTING`
- `LOCKED`
- `FULFILLING`
- `COMPLETED`
- `CANCELLED`

Suggested quote states:

- `SUBMITTED`
- `WITHDRAWN`
- `ACCEPTED`
- `REJECTED`

Suggested order states:

- `CREATED`
- `IN_PROGRESS`
- `WAITING_DOCUMENT`
- `WAITING_EXPORT`
- `COMPLETED`
- `CANCELLED`

### News

Main entities:

- `news_article`
- `news_article_content`
- `news_comment`
- `news_like_record`
- `news_collect_record`
- `news_sync_task`

Suggested article states:

- `PULLED`
- `PUBLISHED`
- `OFFLINE`

## Core User Flows

### Flow A: Publish Freight Demand

1. user creates demand
2. uploads attachments to MinIO
3. platform stores demand and attachment metadata
4. demand enters `PUBLISHED`
5. async event updates hot list, search index, and notifications

### Flow B: Quote and Accept

1. agent submits quote
2. publisher selects one quote
3. system creates fulfillment order
4. acceptance event triggers async follow-up tasks
5. fulfillment order can later invoke template or bill capabilities

### Flow C: News Pull and Interaction

1. scheduled job pulls free news API data
2. duplicate articles filtered by external id or normalized URL hash
3. article stored and optionally summarized or tagged
4. Redis updates hot/article cache
5. users browse, like, comment, collect
6. interaction counts are written asynchronously through MQ

## API and Communication Strategy

### Sync Calls

Use `Feign + Nacos` for:

- auth and identity lookup
- homepage aggregation
- lightweight detail queries
- marketplace summary aggregation
- news summary aggregation

### Async Calls

Keep `RabbitMQ` for:

- heavy document tasks
- search sync
- ranking updates
- notification fan-out
- delayed business actions
- interaction count flush
- order follow-up events

Feign should remain control-plane oriented. RabbitMQ should remain workload-plane oriented.

## High-Concurrency Design

### Marketplace Hot Demand Acceptance

Use Redis-first protection for popular demands:

- maintain demand availability and current acceptance state in Redis
- use Redis lock or Lua script to avoid duplicate acceptance
- persist final state in DB
- emit acceptance event to MQ

This is the shipping-domain equivalent of a high-traffic purchase or order-grab scenario.

### News Engagement

Use Redis for:

- like state set membership
- article like count
- comment count cache
- hot ranking zset

Periodically or asynchronously flush aggregated counts to MySQL.

### General Platform Controls

Use Redis for:

- interface rate limiting
- idempotency keys
- task polling cache
- hot detail cache
- distributed locks for state transitions

## Search and Observability

### Elasticsearch

Introduce two business indexes:

- freight demand search index
- news article search index

Freight demand search fields:

- title
- goods name
- departure port
- destination port
- shipping date window
- publisher company
- status

News article search fields:

- title
- summary
- tags
- source
- publish time

### Kibana

Use for:

- MQ failure diagnosis
- article sync failure logs
- demand acceptance anomaly tracing
- LLM task trace search
- slow API and gateway request analysis

## Frontend Reconstruction Direction

The frontend should become a single coherent platform with four primary sections:

- platform home
- shipping tools
- freight market
- news community

### Platform Home

Home should show:

- quick entry to bill/template tools
- hot freight demands
- latest shipping news
- task center summary

### Shipping Tools

Keep and refine:

- bill extraction
- template extraction
- template save
- template export
- task polling and result viewing

### Freight Market

Key screens:

- freight demand list
- demand detail
- publish demand
- my published demands
- my quotes
- my fulfillment orders

### News Community

Key screens:

- news feed
- article detail
- comment panel
- my collections
- hot article ranking

## Rollout Plan

### Stage 1: Marketplace Backend First

Deliver:

- `service-market`
- demand publish/list/detail
- quote submit
- accept quote
- fulfillment order creation
- Redis anti-duplicate protection
- RabbitMQ follow-up events

### Stage 2: Marketplace Frontend Slice

Deliver:

- market pages in client frontend
- publish form
- list and detail pages
- quote and accept interactions

### Stage 3: News Backend

Deliver:

- `service-news`
- free news provider integration
- scheduled sync
- article list/detail
- comment/like/collect
- Redis hot ranking

### Stage 4: News Frontend Slice

Deliver:

- news list
- article detail
- interaction UI

### Stage 5: Unified Frontend Reconstruction

Deliver:

- integrated home
- unified navigation
- visual consolidation
- platform cards, dashboards, and tool entries

## Verification Strategy

Each stage must include:

- unit tests for service logic
- controller or integration tests for main APIs
- local smoke tests with real HTTP calls
- queue verification for async paths
- health checks for new services

For marketplace specifically:

- publish demand happy path
- repeat acceptance protection
- quote acceptance state transition
- order creation event verification

For news specifically:

- article sync deduplication
- comment create path
- like idempotency
- hot ranking update path

## Non-Goals

- not building a full payment platform
- not building generic social networking
- not replacing the existing shipping tools with marketplace pages
- not making LLM-heavy tasks synchronous

## Recommendation

Proceed with implementation in this order:

1. write implementation plan for `service-market`
2. build and verify marketplace backend
3. expose frontend market entry
4. then add `service-news`
5. then rebuild the client into the integrated platform
