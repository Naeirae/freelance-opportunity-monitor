# Domain contracts

This document defines the first stable contracts between the Financial Control Center core and optional modules.

The goal is to avoid coupling Android screens directly to the implementation details of job search, investments, taxes or document extraction.

## Money

```kotlin
data class MoneyAmount(
    val minorUnits: Long,
    val currency: String
)
```

Use integer minor units where practical. For currencies without 2-decimal minor units or when source statements contain higher precision, the backend may use `numeric` and expose a decimal string over API.

## Confidence

```kotlin
enum class ConfidenceLevel {
    CONFIRMED,
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}
```

Confirmation describes evidence, not desirability.

## Opportunity

Universal candidate for future economic benefit.

```text
id
kind
source_module
name
nominal_amount
currency
probability_percent
time_to_money_days
cash_required
work_hours_estimate
scalability
risk_level
status
next_action
source_url
created_at
updated_at
```

Suggested `kind` values:

- freelance_order
- job_opening
- tax_refund
- benefit
- compensation
- bank_refund
- legal_claim
- investment_optimization
- digital_product
- other

## Expected inflow

```text
id
name
amount
currency
expected_date
confidence
source_object_type
source_object_id
status
```

Statuses:

- expected
- delayed
- received
- cancelled

## Obligation

```text
id
name
kind
principal_or_amount_due
currency
next_due_date
minimum_payment
rate_percent
penalty_description
grace_until
prepayment_allowed
status
severity_if_missed
source_document_id
```

## Reservation / protected liquidity

```text
id
name
amount
currency
from_date
until_date
priority
hardness
reason
```

`hardness`:

- hard - engine must not allocate automatically;
- protected - may be touched only with explicit warning/override;
- soft - scenario may use it but must show impact.

## Coverage link

Links a source of money to an obligation.

```text
id
obligation_id
source_type
source_id
amount
currency
confidence
expected_date
```

Coverage can point to:

- current_allocatable_cash
- expected_inflow
- opportunity
- reserve_override
- manual_source

## Asset snapshot

```text
id
asset_id
name
kind
value
currency
as_of
liquidity_level
source_module
```

## Claim

Claims may be represented as opportunities but deserve dedicated legal metadata.

```text
id
name
counterparty
nominal_amount
currency
probability_percent
deadline
legal_basis_summary
evidence_state
status
next_action
```

## Action candidate

Modules do not tell the user what to do directly. They may submit action candidates to the central decision engine.

```text
id
producer_module
action_type
title
cash_required
currency
guaranteed_saving
expected_gain
probability_percent
liquidity_after_action
risk_level
time_cost_hours
deadline
explanation
related_object_ids
```

Examples:

- prepay 5,000 RUB on a loan;
- preserve 5,000 RUB as reserve;
- pay a credit card before grace expiration;
- spend 3,000 RUB on a tool needed for a confirmed 15,000 RUB job;
- switch broker tariff to avoid monthly fees;
- file a tax claim before a deadline.

## Integration event envelope

```json
{
  "event_id": "uuid",
  "event_type": "opportunity.created",
  "occurred_at": "2026-08-23T12:00:00Z",
  "producer": "freelance-monitor",
  "schema_version": 1,
  "user_id": "uuid",
  "payload": {}
}
```

Modules must include a schema version. The core should tolerate additive fields and reject incompatible major changes.

## Modules

### Opportunity providers

Examples: Kwork monitor, HH/LinkedIn helper, other freelance sources.

Required capability:

```text
publish opportunity
update opportunity status
convert won opportunity -> expected inflow
```

### Investment providers

Required capability:

```text
publish asset snapshot
publish fee/risk opportunity
publish tax optimization opportunity
publish action candidate
```

No trading side effects are part of this contract.

### Tax/benefit/claim providers

Required capability:

```text
publish opportunity/claim
update status
attach deadline
attach evidence/source documents
```

### Capture clients

Android and Telegram may create drafts. Drafts are validated by core before canonical writes.

## API design consequence

Android should depend on DTOs/repositories representing these contracts, not on Kwork-specific or broker-specific classes.

A future screen can therefore show:

- a freelance order;
- a tax refund;
- a broker fee saving;
- a legal compensation claim;

in the same `Opportunities` feed while preserving specialized detail screens.
