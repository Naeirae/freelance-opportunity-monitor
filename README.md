# Freelance Opportunity Monitor

A small Python tool for collecting freelance opportunities, scoring them by expected value, and sending the best ones to Telegram.

The first goal is practical: reduce time spent browsing marketplaces and surface short, reasonably paid tasks that are worth applying to.

## MVP

- Store opportunities in SQLite
- Import opportunities manually or from JSON
- Score by budget, estimated hours, relevance, urgency and risk flags
- Filter out low-value work
- Send a compact alert to Telegram
- Keep source adapters separate so Kwork and other marketplaces can be added without changing the core logic

## Current status

The repository is at MVP stage. Automatic authenticated actions on freelance marketplaces are intentionally out of scope. The tool is designed around **read/filter/notify**, with the final application sent manually by the user.

## Quick start

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
cp .env.example .env
python -m fom.cli init-db
python -m fom.cli add \
  --source kwork \
  --title "Telegram bot + Google Sheets" \
  --url "https://example.com/project" \
  --budget 7000 \
  --hours 4 \
  --description "Python bot, one integration"
python -m fom.cli list
```

## Scoring idea

The score is deliberately simple and explainable. It favors:

- higher budget per hour;
- tasks matching Python / automation / AI / Excel / Google Sheets / Telegram;
- short delivery windows;
- low ambiguity;
- low scam / platform-risk signals.

The score is not meant to decide automatically. It is a triage layer that helps find the best opportunities first.

## Environment variables

See `.env.example`.

For Telegram alerts:

- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`

## Roadmap

1. Public Kwork project collector (no login, no automated bidding)
2. AI-assisted estimate of complexity and hours
3. Draft application text generator
4. Multiple marketplaces
5. Simple web dashboard
6. Historical conversion statistics: viewed -> applied -> reply -> paid

## Safety / account protection

This project does not automate login, messaging, bidding, captcha solving, or rate-limit bypassing. Marketplace-specific collectors should respect public access rules and terms of service.

## License

MIT
