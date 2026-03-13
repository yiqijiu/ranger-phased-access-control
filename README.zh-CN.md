<div align="center">

# Ranger Phased Access Control

### 面向 Hive / Spark / Doris 的分阶段权限接入与统一安全治理框架

<p align="center">
  <img src="https://img.shields.io/badge/architecture-centralized-blue" />
  <img src="https://img.shields.io/badge/engines-Hive%20%7C%20Spark%20%7C%20Doris-success" />
  <img src="https://img.shields.io/badge/policy-BLOCK%20%7C%20BYPASS%20%7C%20WARN%20%7C%20CHECK-orange" />
  <img src="https://img.shields.io/badge/availability-fail--open-critical" />
  <img src="https://img.shields.io/badge/status-initial%20design-lightgrey" />
  <img src="https://img.shields.io/badge/license-MIT-black" />
</p>

<p align="center">
  分阶段接入 · 中心化决策 · 插件哑终端化 · IM 软件通知 · 熔断放行
</p>

</div>

---

## 项目简介

**Ranger Phased Access Control** 是一个面向 Hive、Spark、Doris 等大数据引擎的分阶段权限接入与统一安全治理框架。

它通过把复杂治理逻辑收敛到统一治理平台，将组件侧插件简化为“上下文采集 + 平台调用 + 动作路由”的轻量执行层，帮助团队在不牺牲业务连续性的前提下，实现从存量任务豁免到白名单准入的平滑演进。

---

## 为什么需要这个项目

大数据权限治理真正困难的往往不是“有没有 Ranger”，而是“如何把 Ranger 平稳上线”。

典型问题包括：

- 历史任务体量大，无法一刀切全量接入
- 无名任务、黑户任务较多，缺乏归属信息和治理抓手
- Hive、Spark、Doris 各自维护逻辑，治理成本高且不一致
- 鉴权链路一旦变重，容易影响线上性能和可用性

本项目的目标就是把复杂策略上移到平台，把执行逻辑下沉成统一协议，让治理能力可灰度、可回滚、可扩展。

---

## 核心能力

### 1. 中心化决策

组件插件只负责采集上下文并调用治理平台，真正的规范检查、分流策略、治理决策和通知闭环都由平台统一完成。

### 2. 四类标准动作

平台返回以下四类动作，组件端只负责执行：

- `BLOCK`：强阻断
- `BYPASS`：直接放行
- `WARN`：提醒放行
- `CHECK`：进入 Ranger 原生鉴权

### 3. 分阶段接入 Ranger

不是让所有任务一次性进入 Ranger，而是通过平台策略实现：

- 存量任务默认 `BYPASS`
- 过渡期任务可走 `WARN`
- 白名单或已纳管任务走 `CHECK`
- 未注册或违规任务走 `BLOCK`

### 4. IM 软件通知

当发现无名任务、违规任务或权限缺失任务时，平台可以异步触发 IM 软件通知，把拦截原因、整改建议和责任归属推送给用户或项目组，形成治理闭环。

### 5. 熔断放行

当治理平台不可用时，组件端自动降级到 `BYPASS`，优先保障业务连续性。

---

## 总体架构

```mermaid
flowchart LR
    A[Client / SQL Request] --> B[Engine Plugin<br/>Hive / Spark / Doris]
    B --> C[Governance Platform]
    C --> D[Policy Decision]
    C --> E[Async IM Notification]
    D --> F{ActionType}
    F -->|BLOCK| G[Reject]
    F -->|BYPASS| H[Allow]
    F -->|WARN| I[Allow + Notify]
    F -->|CHECK| J[Ranger Authorization]
    J -->|Pass| K[Execute]
    J -->|Fail| L[msgFaill Callback]
    L --> C
