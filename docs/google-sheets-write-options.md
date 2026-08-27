# Google Sheets write options for Market Research Companion

## Goal

Record structured market-demand signals from a browser extension into the finance research spreadsheet without embedding Google account credentials in the extension.

Target spreadsheet: `15 — SEO и анализ спроса`.

## What open-source implementations show

### 1. Direct Google Sheets API / OAuth

Large projects such as Home Assistant use an OAuth session, validate the token/scopes, then pass the access token to a Sheets client. The write path is ultimately an append operation. This is the cleanest multi-user architecture, but a public Chrome extension needs a Google Cloud OAuth client, consent-screen configuration and potentially additional review for requested scopes.

### 2. Python + gspread

`gspread` is a thin Python wrapper around the Google Sheets API. Typical code opens a spreadsheet by key, selects a worksheet and calls `append_row` / `append_rows`. Home Assistant uses this pattern and also reads the header row first, maps object keys to columns, adds missing headers, then appends rows.

Useful properties:

- easy service-account or OAuth integration;
- batch append support;
- values can be written as `USER_ENTERED`;
- application code can validate/deduplicate before writing.

Do not put a service-account private key in a browser extension.

### 3. Google Apps Script web endpoint

For a private/personal tool, a small Apps Script Web App is the lowest-friction bridge:

`Chrome extension -> HTTPS POST -> Apps Script -> SpreadsheetApp.appendRow()`

The script runs under the owner's Google account. A shared secret can be stored in Apps Script Properties; the extension stores its copy only in `chrome.storage.local`.

Advantages for this project:

- no paid server;
- no Google OAuth flow inside the extension;
- no service-account JSON in client code;
- easy to point at one known spreadsheet and one known tab;
- can validate schema and reject malformed rows.

Limitations:

- best for this private research tool, not a public multi-user SaaS;
- Apps Script quotas and deployment permissions apply;
- a client-side secret is not strong protection against a determined attacker, so the endpoint must never expose privileged operations beyond appending validated research rows.

## Chosen MVP architecture

Use Google Apps Script as a narrow append-only ingest endpoint.

```text
active browser tab
      |
      v
Market Research Companion
extract title/url/domain/query + manual ratings
      |
      v
POST JSON + local token
      |
      v
Apps Script Web App
validate token + schema
      |
      v
15 — SEO и анализ спроса
append one row
```

The browser extension never receives Google credentials and never gets arbitrary read/write access to Drive.

## Production evolution

If this becomes a public product, replace Apps Script with a proper backend (Cloudflare Worker / Python service) and use OAuth or a server-side service account as appropriate. Keep the extension contract unchanged: it posts a versioned `MarketSignal` object to an ingest API.

## Reliability rules

- every signal receives a stable `signal_id` generated in the extension;
- the endpoint should reject missing required fields;
- append result must return an explicit success/failure payload;
- the extension keeps unsent signals locally until success;
- retries reuse the same `signal_id`;
- later, deduplicate by `signal_id` before append;
- never report success merely because `fetch()` returned: parse the endpoint response and confirm `ok: true`.
