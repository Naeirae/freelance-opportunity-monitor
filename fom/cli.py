from __future__ import annotations

import argparse

from dotenv import load_dotenv

from .db import add_opportunity, init_db, list_opportunities
from .drafts import build_application_draft
from .models import Opportunity
from .scoring import score_opportunity
from .telegram import format_alert, send_alert


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="fom")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("init-db")

    add = sub.add_parser("add")
    add.add_argument("--source", required=True)
    add.add_argument("--title", required=True)
    add.add_argument("--url", required=True)
    add.add_argument("--budget", type=int)
    add.add_argument("--hours", type=float)
    add.add_argument("--description", default="")
    add.add_argument("--published-at")

    listing = sub.add_parser("list")
    listing.add_argument("--min-score", type=float, default=-999)

    notify = sub.add_parser("notify")
    notify.add_argument("--min-score", type=float, default=35)

    drafts = sub.add_parser("drafts")
    drafts.add_argument("--min-score", type=float, default=35)
    drafts.add_argument("--limit", type=int, default=5)

    return parser


def _ranked(min_score: float):
    ranked = []
    for item in list_opportunities():
        result = score_opportunity(item)
        if result.score >= min_score:
            ranked.append((result.score, item, result))
    ranked.sort(key=lambda row: row[0], reverse=True)
    return ranked


def main() -> None:
    load_dotenv()
    args = build_parser().parse_args()

    if args.command == "init-db":
        init_db()
        print("Database initialized")
        return

    if args.command == "add":
        item = Opportunity(
            source=args.source,
            title=args.title,
            url=args.url,
            budget_rub=args.budget,
            estimated_hours=args.hours,
            description=args.description,
            published_at=args.published_at,
        )
        item_id = add_opportunity(item)
        result = score_opportunity(item)
        print(f"Saved #{item_id}; score={result.score}")
        return

    ranked = _ranked(args.min_score)

    if args.command == "list":
        for score, item, _ in ranked:
            budget = item.budget_rub if item.budget_rub is not None else "?"
            print(f"{score:>5} | {budget} RUB | {item.title} | {item.url}")
        return

    if args.command == "notify":
        for _, item, result in ranked:
            send_alert(format_alert(item, result))
        print(f"Sent {len(ranked)} alerts")
        return

    if args.command == "drafts":
        for score, item, result in ranked[: args.limit]:
            print(f"\n=== {score} | {item.title} ===")
            print(item.url)
            print(build_application_draft(item, result))


if __name__ == "__main__":
    main()
