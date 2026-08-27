# Market Research Companion MVP

Chrome Manifest V3 extension for capturing market-demand signals from the active browser tab and writing them directly to the finance research Google Sheet via Google Sheets API.

## What is implemented

- Google OAuth through `chrome.identity.getAuthToken()`;
- direct Google Sheets API access from the extension;
- no Google password/cookies/service-account key in the extension;
- local `chrome.storage.local` outbox;
- stable `Signal ID` per captured record;
- append via Sheets API;
- mandatory reread of the exact `updatedRange` returned by the API;
- byte-for-byte-ish string comparison of expected vs reread cell values before the signal is removed from the outbox;
- automatic capture of active page URL, title and domain;
- manual market-research classification fields.

Target spreadsheet currently defaults to:

`15 — SEO и анализ спроса`

Spreadsheet ID:

`1sOpk8dQB9VCK4n3YK7vQ760Y-L4oxulavZHgqcdedOI`

Target worksheet: `Сигналы`.

## One-time Google setup

This extension intentionally does not use browser cookies as credentials. Chrome can reuse the signed-in Chrome/Google identity, but Google still requires an OAuth client that grants the extension the Sheets scope.

1. Open Google Cloud Console.
2. Create/select a project for this private extension.
3. Enable **Google Sheets API**.
4. Configure the OAuth consent screen for your own account/testing.
5. In Chrome open `chrome://extensions`, enable Developer mode, load this folder as an unpacked extension and copy its Extension ID.
6. In Google Cloud Credentials create an OAuth client for a **Chrome extension** and enter that Extension ID.
7. Copy the generated OAuth client ID into `manifest.json` replacing:

   `REPLACE_WITH_CHROME_EXTENSION_OAUTH_CLIENT_ID.apps.googleusercontent.com`

8. Reload the extension in `chrome://extensions`.
9. Open the popup and press **Подключить Google**.

The OAuth client ID is not a password and is expected to be present in a Chrome extension manifest. Do not add client secrets, refresh tokens or service-account private keys to the repository.

## First test

1. Confirm the spreadsheet contains the expected header row in `Сигналы`.
2. Open any harmless public page.
3. Open the extension popup.
4. Fill at least Query / Market / Language.
5. Click **Сохранить сигнал**.
6. Success is reported only if:
   - the append API call returns an `updatedRange`;
   - the extension rereads that exact range;
   - the reread values match the values sent.

If any stage fails, the signal remains in the local outbox. Use **Повторить отправку очереди** after fixing the problem.

## Current sheet schema

`Date | Market | Language | Query | Query family | Source | Result type | URL | Domain | Title | Commercial intent 0–3 | Pain signal 0–3 | AI resistance 0–5 | Competitor | Price | Key promise | Gap / complaint | Evidence note | Status | Next check | Signal ID`

## Security note

The current MVP requests the `spreadsheets` OAuth scope, which can access Google Sheets that the signed-in user can access. This is acceptable for the private prototype, but before public distribution we should reduce privileges if the product flow allows it and review Google's OAuth verification requirements.

## Next engineering steps

1. Test real OAuth and append against the finance sheet.
2. Add duplicate detection by `Signal ID` before append, not only stable retry IDs.
3. Detect search-engine query text automatically where reliable.
4. Add one-click extraction helpers for Google/Bing/Reddit/GitHub/Product Hunt/Chrome Web Store.
5. Add batch capture of selected search results only after confirming terms/rate-limit safety.
6. Add demand-analysis summaries computed from the sheet.
