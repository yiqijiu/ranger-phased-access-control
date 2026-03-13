<div align="center">

# Ranger Phased Access Control

### Hive / Spark / Doris 统一安全治理（Java 实现）

</div>

## 核心理念

- 中心化决策
- 插件哑终端化
- 飞邮闭环触达
- 熔断放行

## Maven 模块规划

- `java/common`：统一协议模型与动作枚举
- `java/server`：治理平台服务端（Java）
- `java/hive-plugin`：Hive 组件插件路由实现（Java）
- `java/spark-plugin`：Spark 插件占位模块（Java）
- `java/doris-plugin`：Doris 插件占位模块（Java）

## 标准动作（ActionType）

- `BLOCK`：抛异常拦截 SQL
- `BYPASS`：直接放行，跳过 Ranger
- `WARN`：提醒放行（平台异步飞邮）
- `CHECK`：进入 Ranger 原生鉴权

## 标准交互协议

请求报文：

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

响应报文：

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

## Hive 插件路由逻辑

`PhasedRangerAuthorizer`：

1. 调用治理平台 `decide`
2. 平台异常或非 200：触发熔断放行（BYPASS）
3. 按动作执行：
   - `BLOCK`：抛出 `AccessControlException(msg)`
   - `BYPASS/WARN`：直接 return
   - `CHECK`：调用 Ranger；失败时回调 `msgFail`
4. `CHECK` 失败支持双模式：
   - 严管模式（默认）：继续抛出 Ranger 异常并阻断
   - 观察模式：吞掉 Ranger 异常，仅回调治理平台（只测不拦）

## 构建与测试

```bash
mvn test
```

## Hive / Spark / Doris 分阶段路由实现

为继承 Ranger 原生能力，Hive 插件新增：

- `PhasedRangerAuthorizer extends RangerHiveAuthorizer`
- `PhasedRangerAuthorizerFactory extends RangerHiveAuthorizerFactory`

执行路由：

- `BLOCK`：抛出 `HiveAccessControlException`
- `BYPASS/WARN`：直接放行
- `CHECK`：调用 `super.checkPrivileges(...)` 走 Ranger 原生鉴权

通过自定义 Factory 替换原生 Factory，在保留 Ranger 能力的同时插入治理平台决策路由。


Spark 与 Doris 也已改为同样的分阶段路由结构（`PhasedSparkAuthorizer` / `PhasedDorisAuthorizer` + Factory）。
=======

- 中心化决策
- 插件哑终端化
- 飞邮闭环触达
- 熔断放行

## Maven 模块规划

- `java/common`：统一协议模型与动作枚举
- `java/server`：治理平台服务端（Java）
- `java/hive-plugin`：Hive 组件插件路由实现（Java）
- `java/spark-plugin`：Spark 插件占位模块（Java）
- `java/doris-plugin`：Doris 插件占位模块（Java）

## 标准动作（ActionType）

- `BLOCK`：抛异常拦截 SQL
- `BYPASS`：直接放行，跳过 Ranger
- `WARN`：提醒放行（平台异步飞邮）
- `CHECK`：进入 Ranger 原生鉴权

## 标准交互协议

请求报文：

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

响应报文：

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

## Hive 插件路由逻辑

`HiveGovernanceAuthorizer`：

1. 调用治理平台 `decide`
2. 平台异常或非 200：触发熔断放行（BYPASS）
3. 按动作执行：
   - `BLOCK`：抛出 `AccessControlException(msg)`
   - `BYPASS/WARN`：直接 return
   - `CHECK`：调用 Ranger；失败时回调 `msgFail`
4. `CHECK` 失败支持双模式：
   - 严管模式（默认）：继续抛出 Ranger 异常并阻断
   - 观察模式：吞掉 Ranger 异常，仅回调治理平台（只测不拦）

## 构建与测试

```bash
mvn test
```
