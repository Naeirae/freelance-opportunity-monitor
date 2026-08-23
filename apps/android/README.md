# FinControl Android

Android is the primary client for the Financial Control Center. The cloud database will be authoritative; this app will later keep an offline cache and synchronize through the backend API.

## Current prototype

The first dashboard intentionally uses demo data only. Do not commit real personal financial values to this public repository.

Implemented in the prototype:

- available / protected / allocatable money;
- large obligations;
- expected income;
- attention items;
- core domain model;
- protected-liquidity allocation calculator.

## Next implementation order

1. Add/edit significant financial object.
2. Protected liquidity editor.
3. Coverage links: which income/source covers which obligation.
4. Scenario screen: allocate X without breaching protected liquidity.
5. Local Room cache.
6. Backend API client and auth.
7. Cloud synchronization.
8. Document/photo intake and AI proposal review.

## Privacy rule

The repository is public. Source code and synthetic/demo fixtures belong here; real statements, names, account numbers, debts and personal financial records do not. Production data must live only in the authenticated cloud environment.