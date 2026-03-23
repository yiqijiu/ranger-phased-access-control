<div align="center">

# Ranger Phased Access Control

### Unified Security Governance for Hive / Spark / Doris (Java-first)

</div>

## Core Principles
- Centralized decision making
- Dumb plugin routing (engine-side lightweight)
- Msg closed-loop notification
- Fail-open for business continuity

## Modules (Maven Multi-Module)

- `java/common`: shared protocol & action enums
- `java/server`: governance decision service (Java)
- `java/hive-plugin`: Hive plugin-side action router (Java)
- `java/spark-plugin`: Spark plugin placeholder (Java)
- `java/doris-plugin`: Doris plugin placeholder (Java)

## Standard ActionType

- `BLOCK`: throw exception and stop SQL execution
- `BYPASS`: allow directly, skip Ranger
- `WARN`: allow with warning (platform sends msg)
- `CHECK`: invoke Ranger authorization

## Request/Response Contract

Request:

```json
{
  "jobName": "etl_finance_daily_01",
  "user": "zhangsan",
  "engine": "HIVE_TEZ",
  "sql": "SELECT * FROM finance.orders LIMIT 10",
  "clientIp": "10.1.100.5",
  "queryId": "hive_xxxx"
}
```

Response:

```json
{
  "code": 200,
  "traceId": "req-8f7a9b2c",
  "data": {
    "actionType": "CHECK",
    "msg": "",
    "alertTriggered": false,
    "queryId": "hive_xxxx"
  }
}
```

## Hive Plugin Routing Semantics
`PhasedRangerAuthorizer` behavior:

1. call governance platform (`GovernanceClient.decide`)
2. if exception / non-200 response: fail-open (`BYPASS`)
3. route by action:
   - `BLOCK`: throw `AccessControlException` with `msg`
   - `BYPASS/WARN`: return directly
   - `CHECK`: call Ranger delegate; on denial callback `msgFail`
4. `CHECK` failure supports two modes:
   - observe mode (default): catch Ranger `Exception`, report via `msgFail`, then allow query to continue
   - strict mode: report via `msgFail`, then rethrow Ranger denial and block
   - switch by Hive conf `ranger.governance.observe.check.failure` (`true` default observe, `false` strict)
   - rollout recommendation: keep observe mode during legacy SQL remediation to avoid changing current execution flow

## Build & Test

```bash
mvn test
```

## Common HTTP Client (High Concurrency)

`java/common` now provides a pooled HTTP client implementation in `GovernanceClientHttpImpl`:

- Reuses Apache HttpClient (Hive runtime commonly includes this stack)
- Uses connection pooling for concurrent plugin requests
- Uses short, configurable timeouts to avoid impacting Hive SQL execution flow

System properties:

- `governance.client.base-url` (default `http://127.0.0.1:8080`)
- `governance.client.decide-path` (default `/api/v1/governance/decision`)
- `governance.client.msg-fail-path` (default `/api/v1/governance/msg-fail`)
- `governance.client.connect-timeout-ms` (default `200`)
- `governance.client.socket-timeout-ms` (default `500`)
- `governance.client.connection-request-timeout-ms` (default `100`)
- `governance.client.max-total` (default `512`)
- `governance.client.max-per-route` (default `256`)
- `governance.client.keep-alive-ms` (default `30000`)

## Server Module (PostgreSQL + Whitelist Admin Page)

`java/server` is now a Spring Boot service with PostgreSQL persistence.

- DB migration: `java/server/src/main/resources/db/migration/V1__create_task_whitelist.sql`
- Whitelist admin page: `GET /admin/task-whitelist`
- Whitelist API:
  - `GET /api/v1/task-whitelist`
  - `POST /api/v1/task-whitelist`
  - `PUT /api/v1/task-whitelist/{id}`
  - `DELETE /api/v1/task-whitelist/{id}`
- Governance API:
  - `POST /api/v1/governance/decision`
  - `POST /api/v1/governance/msg-fail`

Default decision policy in server:

- blocked user or invalid jobName: `BLOCK`
- whitelisted taskName: `CHECK`
- legacy taskName not in whitelist: `BYPASS`

Run server module with local PG:

```bash
mvn -pl java/server spring-boot:run
```

Environment variables:

- `PG_URL` (default `jdbc:postgresql://localhost:5432/governance`)
- `PG_USER` (default `governance`)
- `PG_PASSWORD` (default `governance`)
- `SERVER_PORT` (default `8080`)

Notification channels (Feishu / Slack / DingTalk / WeCom / Teams webhook):

- `governance.notify.channel` (default `noop`, supported `noop|feishu|slack|dingtalk|wecom|teams`)
- `governance.notify.connect-timeout-ms` (default `200`)
- `governance.notify.read-timeout-ms` (default `1000`)
- `governance.notify.feishu.webhook-url` (or env `FEISHU_WEBHOOK_URL`)
- `governance.notify.slack.webhook-url` (or env `SLACK_WEBHOOK_URL`)
- `governance.notify.dingtalk.webhook-url` (or env `DINGTALK_WEBHOOK_URL`)
- `governance.notify.wecom.webhook-url` (or env `WECOM_WEBHOOK_URL`)
- `governance.notify.teams.webhook-url` (or env `TEAMS_WEBHOOK_URL`)

Behavior:

- only one notifier channel is instantiated at runtime
- `channel=noop` falls back to local noop logging
- if a concrete channel is selected but webhook is missing, startup fails fast
- notification send failure is logged and decision flow continues

## Ranger Inheritance Integration (Hive / Spark / Doris)

To preserve native Ranger capabilities, Hive plugin now provides:

- `PhasedRangerAuthorizer extends RangerHiveAuthorizer`
- `PhasedRangerAuthorizerFactory extends RangerHiveAuthorizerFactory`

Routing behavior:

- `BLOCK`: throw `HiveAccessControlException`
- `BYPASS/WARN`: return directly
- `CHECK`: call `super.checkPrivileges(...)` to reuse Ranger native policy

Factory returns phased authorizer instead of native factory output, so governance routing is inserted before Ranger check while keeping Ranger compatibility.


Spark and Doris follow the same phased routing contract via `PhasedSparkAuthorizer` and `PhasedDorisAuthorizer` plus their factory classes.

## Hive Plugin HTTP Config

Hive plugin now includes default HTTP config file:

- `java/hive-plugin/src/main/resources/ranger-governance-http.properties`

Supported parameters:

- `governance.client.base-url`
- `governance.client.decide-path`
- `governance.client.msg-fail-path`
- `governance.client.connect-timeout-ms`
- `governance.client.socket-timeout-ms`
- `governance.client.connection-request-timeout-ms`
- `governance.client.max-total`
- `governance.client.max-per-route`
- `governance.client.keep-alive-ms`

Load priority in Hive runtime:

1. JVM `-Dgovernance.client.*` system properties
2. `hive-site.xml` (`HiveConf`) with same keys
3. plugin bundled `ranger-governance-http.properties`
