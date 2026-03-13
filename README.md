<div align="center">

# Ranger Phased Access Control

### Unified Security Governance for Hive / Spark / Doris

<p align="center">
  <img src="https://img.shields.io/badge/architecture-centralized-blue" />
  <img src="https://img.shields.io/badge/engines-Hive%20%7C%20Spark%20%7C%20Doris-success" />
  <img src="https://img.shields.io/badge/policy-BLOCK%20%7C%20BYPASS%20%7C%20WARN%20%7C%20CHECK-orange" />
  <img src="https://img.shields.io/badge/availability-fail--open-critical" />
  <img src="https://img.shields.io/badge/status-initial%20design-lightgrey" />
  <img src="https://img.shields.io/badge/license-MIT-black" />
</p>

<p align="center">
  Phased Adoption · Centralized Decision-Making · Dumb Plugins · IM Notification · Fail-Open Protection
</p>

</div>

---

## Overview

**Ranger Phased Access Control** is a phased access control and unified security governance framework for big data engines such as Hive, Spark, and Doris.

It centralizes governance logic in a dedicated platform while keeping engine-side plugins lightweight and execution-focused. This allows teams to evolve from legacy bypass mode to whitelist-based admission control without risky all-at-once Ranger rollout.

---

## Why This Project

The real challenge in data platform authorization is usually not whether Ranger exists, but how to roll it out safely in production.

Common problems include:

- legacy workloads cannot be migrated all at once
- anonymous or unowned jobs are hard to track and govern
- Hive, Spark, and Doris often implement governance differently
- authorization logic can easily become part of the critical path

This project addresses those issues by moving complex policies to a centralized governance layer and reducing plugins to context collection, platform invocation, and action routing.

---

## Core Capabilities

### 1. Centralized Decision-Making

Plugins only collect runtime context and call the governance platform. Naming checks, migration routing, policy decisions, and notification workflows are all centralized.

### 2. Four Standard Actions

The governance platform returns one of four standard actions:

- `BLOCK`
- `BYPASS`
- `WARN`
- `CHECK`

### 3. Phased Ranger Adoption

Instead of forcing all workloads through Ranger immediately, the platform enables gradual rollout:

- legacy jobs can remain in `BYPASS`
- transitional jobs can run with `WARN`
- approved or whitelisted jobs go through `CHECK`
- invalid or unregistered jobs are `BLOCK`ed

### 4. IM Notification

When anonymous jobs, policy violations, or missing-permission events are detected, the platform can asynchronously send IM notifications to job owners or related teams with remediation guidance, forming a closed-loop governance workflow.

### 5. Fail-Open Protection

If the governance platform becomes unavailable, plugins automatically degrade to `BYPASS`, prioritizing business continuity.

---

## Architecture

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
