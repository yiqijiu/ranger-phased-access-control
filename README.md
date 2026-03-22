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
