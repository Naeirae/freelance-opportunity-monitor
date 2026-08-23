# Financial Control Center - platform architecture

## Product role

This repository is evolving into a modular personal economic operating system. The Android application is the primary client and the Financial Control Center is the central domain. Additional tools - freelance/job discovery, investment monitoring, document analysis, tax/benefit tracking, legal claims, and notifications - should integrate into the same domain model instead of becoming isolated apps.

The core question is not "where did money go?" but:

- what resources are actually available now;
- what amount is protected for life and mandatory commitments;
- what obligations exist and when they become expensive;
- what incoming money is expected or only possible;
- what actions can reduce losses or increase income;
- which action has the best expected economic effect without creating a liquidity gap.

## Architectural rule

Android is a client, not the database owner.

All durable user state should ultimately live in cloud storage behind a versioned API. Android may cache locally for offline use, but the canonical data model must be cloud-first so Telegram, job monitors, investment tools and future interfaces can reuse the same records.

## High-level topology

```text
Android app (primary)
        |
        v
Versioned API / orchestration layer
        |
        +--> Core finance domain
        +--> Liquidity engine
        +--> Coverage engine
        +--> Allocation/scenario engine
        +--> Rules / alerts / deadlines
        +--> AI extraction gateway
        +--> Module integration API
        |
        +--> PostgreSQL
        +--> Object storage
        +--> Event/outbox queue

External/auxiliary modules
        |
        +--> Job and freelance monitor
        +--> Investment monitor
        +--> Tax / benefits / claims tracker
        +--> Telegram capture bot
        +--> Future browser extension
        |
        v
Module integration API / event bus
```

## Core platform domains

### 1. Liquidity

Represents money that exists, but explicitly separates total balances from funds that are safe to allocate.

Important concepts:

- available balance;
- protected liquidity;
- hard reservations;
- soft reservations;
- free-to-allocate amount;
- liquidity horizon;
- minimum life reserve.

Formula at a point in time:

```text
allocatable = available - hard_reservations - protected_liquidity - safety_buffer
```

No recommendation engine may allocate more than `allocatable`.

### 2. Obligations

Examples:

- credit cards;
- installment/consumer loans;
- private IOUs;
- rent;
- school payments;
- taxes;
- mandatory future payments.

Each obligation should support:

- current principal / amount due;
- currency;
- payment schedule;
- next deadline;
- rate / penalty / grace-period metadata;
- prepayment rules;
- source documents;
- coverage plan;
- severity if missed.

### 3. Income and expected inflows

Separate confirmed from probabilistic money.

States:

- recurring confirmed;
- one-off confirmed;
- expected but not yet paid;
- opportunity;
- disputed/claim;
- realized.

### 4. Opportunity

A universal object for money that could exist if an action is taken.

Examples:

- freelance order;
- job opening;
- tax refund opportunity;
- benefit/subsidy;
- bank refund;
- legal claim;
- investment optimization;
- digital product idea.

Useful fields:

- potential amount;
- probability;
- time to money;
- cash investment required;
- labor required;
- legal restrictions;
- scalability;
- automation potential;
- next action.

Expected value can be represented separately from nominal amount.

### 5. Asset

Examples:

- cash and deposits;
- securities;
- investment accounts;
- intellectual property;
- digital products;
- revenue-generating tools.

### 6. Financial event

Large-grained event log. It is intentionally not a transaction ledger.

Examples:

- "Kaspi credit 500,000 KZT opened";
- "T-Bank expensive balance fully repaid and new grace cycle started";
- "tax return filed";
- "new freelance contract accepted for 12,000 RUB";
- "broker access restriction caused inability to manage positions".

One event may reference multiple accounts or documents.

## Coverage engine

Every important obligation may have a coverage plan.

Example:

```text
Obligation: 30,095 KZT due 21 Sep
Coverage:
- salary allocation: 15,000 KZT
- confirmed side work: 8,000 KZT
- still uncovered: 7,095 KZT
```

Coverage sources may be:

- confirmed inflow;
- allocatable current cash;
- expected inflow with probability;
- opportunity not yet realized;
- reserve (explicitly marked as reserve usage).

The engine must distinguish "covered" from "maybe covered".

## Allocation engine

The engine answers questions such as:

- I can freely spend only 5,000 RUB. What is the best use?
- If 20,000 RUB arrives, what changes?
- Should I keep liquidity, prepay debt, fund a work tool, or reserve for a deadline?

Hard guardrails:

1. Never allocate protected liquidity.
2. Never recommend an action that creates a projected cash gap before the next required inflow unless explicitly overridden.
3. Do not assume debt repayment is automatically optimal.
4. Compare guaranteed savings against expected income and liquidity value.
5. Show uncertainty and alternatives.

The result should be a ranked set of actions, not one opaque instruction.

## Scenario engine

Scenarios operate on a snapshot and do not mutate canonical data until confirmed.

Typical scenarios:

- add 5,000 RUB to obligation A;
- keep 5,000 RUB liquid;
- split 20,000 RUB between reserve and debt;
- fund a 3,000 RUB tool expected to support a 15,000 RUB job;
- use a tax refund to close an obligation;
- change a loan from payment-reduction mode to term-reduction mode.

Scenario output:

- post-action liquidity;
- uncovered obligations;
- projected interest/penalties avoided;
- expected income impact;
- risk of cash gap;
- next critical deadline.

## Integration model

Auxiliary tools should integrate using stable contracts.

### Module types

#### Job/freelance module

Produces `Opportunity` objects.

Lifecycle:

```text
Found -> Reviewed -> Applied -> Won -> Expected inflow -> Paid
```

On `Won`, the opportunity can spawn an expected inflow and optionally a work commitment/deadline.

#### Investment module

Produces:

- Asset snapshots;
- investment events;
- fee/leak opportunities;
- tax-loss carryforward opportunities;
- risk alerts;
- scenario candidates.

It must never silently trade. It only informs the central system unless a future explicit trading integration is designed separately.

#### Tax/benefit/legal-claim modules

Produce Opportunities or Claims with:

- nominal amount;
- probability;
- deadline;
- evidence/document links;
- status;
- next action.

#### Telegram bot

Acts as a capture surface and alert channel, not a separate database.

Examples:

- user sends PDF -> extraction draft -> confirm -> create object;
- user writes "client owes 12k on 30 Aug" -> draft expected inflow;
- system sends deadline alert -> deep link opens Android object.

### Integration API principle

All modules should write through the same API and domain validation layer.

Do not allow modules to write directly to PostgreSQL.

Suggested endpoints eventually:

```text
POST /v1/events
POST /v1/opportunities
POST /v1/obligations
POST /v1/inflows
POST /v1/assets/snapshots
POST /v1/documents/extract
POST /v1/scenarios/evaluate
GET  /v1/dashboard
GET  /v1/action-candidates
```

## Event/outbox pattern

Use an outbox table in PostgreSQL so domain changes can safely emit events without losing synchronization.

Examples:

```text
opportunity.created
opportunity.won
inflow.expected
inflow.received
obligation.updated
deadline.approaching
liquidity.changed
scenario.saved
```

Consumers later may include:

- Telegram notifications;
- job monitor workflows;
- investment analytics;
- background recomputation;
- Android push notifications.

A full external message broker is unnecessary for MVP. PostgreSQL outbox is sufficient initially.

## Storage

### PostgreSQL

Canonical structured state.

Candidate platform: Supabase/Postgres for MVP, while keeping SQL and migrations portable.

### Object storage

For:

- PDF statements;
- screenshots;
- contracts;
- tax documents;
- evidence attachments.

Store file metadata and hashes in PostgreSQL.

### Android local storage

Local cache only. Suggested later: Room database containing synchronized projections for offline browsing and pending writes.

## AI boundary

AI is an extraction and suggestion layer, not an authority.

Flow:

```text
Document/text -> extractor -> structured draft -> validation -> user confirmation -> domain write
```

AI should not directly alter debts, balances or deadlines without explicit confirmation, except for low-risk metadata fields if the user later opts in.

## Android-first delivery order

1. Domain models and dashboard projection.
2. Large-object editor.
3. Protected liquidity and free-to-allocate calculation.
4. Obligations + deadlines.
5. Coverage plans.
6. Scenario/allocation screen.
7. Local persistence and repository abstraction.
8. Cloud API + auth + sync.
9. Document upload/extraction.
10. Telegram capture/alerts.
11. Integrate job/freelance module.
12. Integrate investment module.
13. Browser extension only if it solves a proven capture problem.

## Repository strategy

Keep the central platform modular even if the current repository name references freelance monitoring.

Suggested directories over time:

```text
apps/
  android/
services/
  api/
  telegram/
modules/
  opportunities/
  investments/
  taxes/
  claims/
packages/
  domain-contracts/
  scenario-engine/
  integration-contracts/
docs/
```

The current `fom/` package can later become the first implementation of `modules/opportunities/freelance` rather than being deleted.
