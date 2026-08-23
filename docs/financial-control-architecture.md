# Financial Control Center — architecture

## Product goal

This is not a transaction-by-transaction expense tracker. The system stores financially significant objects, deadlines and decisions: debts, protected liquidity, large recurring obligations, expected income, claims, benefits, assets, opportunities and major financial events.

The primary question is: **what money is actually free to allocate, what must be protected, what must be covered next, and which action has the highest expected economic effect without creating a cash-flow gap?**

## Client priority

1. Android app — primary interface and control panel.
2. Telegram bot — fast capture of text, screenshots and documents; later alerts.
3. Browser extension — optional later intake tool for opportunities/pages; not required for MVP.

The database must never live only on Android. Android is an offline-capable client of a cloud system.

## High-level topology

```text
Android app ──────────────┐
                         │
Telegram bot (later) ─────┼── HTTPS API ── domain/application layer ── PostgreSQL
                         │                                │
Browser extension (later) ┘                                ├── object storage (PDF/screenshots)
                                                          ├── AI extraction queue
                                                          ├── scenario engine
                                                          └── reminder/alert engine
```

## Recommended stack

### Android
- Kotlin
- Jetpack Compose
- Material 3
- ViewModel + StateFlow
- Room for offline cache, not as source of truth
- Retrofit/Ktor client for API
- WorkManager for background sync

### Cloud/backend
- Python + FastAPI
- PostgreSQL (initial recommendation: Supabase Postgres)
- Supabase Auth or another token-based auth provider
- Supabase Storage or Cloudflare R2 for source documents
- Background jobs for reminders, extraction and recalculation

### AI boundary
AI never writes authoritative financial state directly.

Flow:
1. document/text arrives;
2. AI returns a structured proposal;
3. backend validates schema;
4. user reviews/edits;
5. only confirmed data becomes authoritative.

Every important object keeps provenance: manual input, document, screenshot, bank statement, URL, etc.

## Core financial rule: protected liquidity

The system distinguishes:

- **available money** — approximate total liquid money;
- **protected money** — money reserved for life and near-term obligations;
- **allocatable money** — the amount that can safely be used for an optional financial action.

Formula:

```text
allocatable = max(0, available - protected)
```

Protected money may be coarse blocks rather than transactions: food/life, housing, school, transport, emergency reserve, already-promised payment.

The allocation engine must never recommend using more than allocatable money unless the user explicitly overrides the protection.

## Domain objects

- MoneyAccount
- ProtectedLiquidityBlock
- Obligation
- ExpectedIncome
- Opportunity
- Claim
- Asset
- IncomeSource
- FinancialEvent
- CoverageLink
- ActionOption
- Scenario
- SourceDocument

## Coverage model

An obligation may be covered by one or more sources.

Example:

```text
Kaspi payment 30,095 KZT
  <- salary 15,000 KZT
  <- freelance 10,000 KZT
  <- other income 5,095 KZT
coverage = 100%
```

The app should show uncovered amount, not merely the obligation balance.

## Allocation engine

Input:
- allocatable amount (for example 5,000 RUB);
- time horizon;
- protected-liquidity floor;
- obligations and deadlines;
- expected income;
- action candidates.

Candidate action dimensions:
- guaranteed financial saving;
- expected additional income;
- avoided penalty/loss;
- liquidity consumed;
- time to result;
- probability;
- reversibility;
- legal/operational risk.

The engine ranks actions but does not execute payments.

## Scenario engine

Typical query: `I have an extra 5,000 / 20,000 / 50,000. What changes?`

Outputs per scenario:
- liquidity left;
- protected-liquidity breach (yes/no);
- debt after action;
- estimated interest/fees avoided;
- uncovered obligations;
- expected additional income;
- next critical date.

## Android MVP screens

1. **Dashboard**
   - Available
   - Protected
   - Free to allocate
   - Next obligations + coverage
   - Expected incoming money
   - Attention required

2. **Objects**
   - obligations
   - income
   - opportunities/claims
   - assets

3. **Add significant item**
   - manual form first
   - document/photo import later

4. **Scenario**
   - amount free to allocate
   - compare 2–4 actions

5. **Timeline / deadlines**
   - only material dates

## Sync model

Cloud is authoritative. Android keeps a Room cache for fast/offline use.

Recommended record fields:
- id (UUID)
- version / updated_at
- deleted_at (soft delete)
- sync_state locally

Sync order:
1. push local pending mutations;
2. receive server changes since last cursor;
3. resolve only explicit conflicts; do not silently overwrite material financial values.

## Security

- no bank passwords/tokens in the app database;
- documents private by default;
- encrypted transport;
- short-lived access tokens;
- per-user row-level authorization;
- secret keys only on backend;
- export/delete account data supported from the start.

## Product boundary for MVP

Out of scope initially:
- automatic bank payments;
- transaction-level budgeting;
- autonomous financial decisions;
- automated credit applications;
- brokerage trading;
- browser extension;
- Kwork auto-bidding.

The first usable product should answer four questions reliably:

1. How much money is safe to allocate right now?
2. Which large obligations are under-covered?
3. What significant money is expected or potentially obtainable?
4. What happens if I allocate X to option A vs B?