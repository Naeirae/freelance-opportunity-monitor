# Freelance Opportunity Monitor

A small Python tool for collecting freelance opportunities, scoring them by expected value, and sending the best ones to Telegram.

The first goal is practical: reduce time spent browsing marketplaces and surface short, reasonably paid tasks that are worth applying to.

## MVP

- Store opportunities in SQLite
- Collect public projects from Kwork without login
- Import opportunities manually
- Score by budget, estimated hours, relevance and risk flags
- Filter out low-value work
- Send compact alerts to Telegram
- Notify only about newly seen Kwork projects when using `collect-kwork --notify`

## Current status

The repository is at MVP stage. Automatic authenticated actions on freelance marketplaces are intentionally out of scope. The tool is designed around **read/filter/notify**, with the final application sent manually by the user.

The Kwork collector reads the public projects page. It does not log in, send proposals, solve captchas or bypass rate limits. Because Kwork can change page internals without notice, the collector should be monitored and its parser tests kept current.

## Quick start

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
cp .env.example .env
python -m fom.cli init-db
```

Fill `.env` with your Telegram credentials:

```text
TELEGRAM_BOT_TOKEN=...
TELEGRAM_CHAT_ID=...
FOM_DB_PATH=data/opportunities.db
KWORK_CATEGORIES=
```

Leave `KWORK_CATEGORIES` empty to scan the general public Kwork projects feed. To restrict the feed, put comma-separated category ids there.

One collection pass without Telegram:

```bash
python -m fom.cli collect-kwork --min-score 20
```

Collect and notify only about newly seen projects whose score is at least 20:

```bash
python -m fom.cli collect-kwork --min-score 20 --notify
```

You can also override categories for one run:

```bash
python -m fom.cli collect-kwork --categories 41 --notify
```

## Scoring idea

The score is deliberately simple and explainable. It favors:

- higher budgets;
- tasks matching Python / automation / AI / Excel / Google Sheets / Telegram;
- short estimated work when an estimate is available;
- low scam / platform-risk signals.

The score is not meant to decide automatically. It is a triage layer that helps find the best opportunities first.

## Environment variables

See `.env.example`.

For Telegram alerts:

- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`

For Kwork:

- `KWORK_CATEGORIES` — optional comma-separated category ids; empty means the general feed.

## Roadmap

1. Run the Kwork collector on a persistent scheduler and verify live stability
2. AI-assisted estimate of complexity and hours
3. Draft application text generator improvements
4. Multiple marketplaces
5. Simple web dashboard
6. Historical conversion statistics: viewed -> applied -> reply -> paid

## Safety / account protection

This project does not automate login, messaging, bidding, captcha solving, or rate-limit bypassing. Marketplace-specific collectors should respect public access rules and terms of service.

## License

MIT
