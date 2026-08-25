from __future__ import annotations

import argparse

from dotenv import load_dotenv

from .db import add_opportunity, init_db, list_opportunities
from .drafts import build_application_draft
from .kwork import categories_from_env, fetch_kwork_projects
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

    collect = sub.add_parser("collect-kwork")
    collect.add_argument(
        "--categories",
        help="Comma-separated Kwork category ids. If omitted, uses KWORK_CATEGORIES; if that is empty, scans the public all-projects feed.",
    )
    collect.add_argument("--page", type=int, default=1)
    collect.add_argument("--min-score", type=float, default=20)
    collect.add_argument("--notify", action="store_true", help="Send Telegram alerts only for newly seen projects above min score")

    return parser


def _ranked(min_score: float):
    ranked = []
    for item in list_opportunities():
        result = score_opportunity(item)
        if result.score >= min_score:
            ranked.append((result.score, item, result))
    ranked.sort(key=lambda row: row[0], reverse=True)
    return ranked


def _parse_categories(raw: str | None) -> list[int]:
    if raw is None:
        return categories_from_env()
    return [int(token.strip()) for token in raw.split(",") if token.strip()]


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

    if args.command == "collect-kwork":
        existing_urls = {item.url for item in list_opportunities()}
        projects = fetch_kwork_projects(_parse_categories(args.categories), page=args.page)

        new_count = 0
        notified = 0
        ranked_new: list[tuple[float, Opportunity]] = []
        for item in projects:
            is_new = item.url not in existing_urls
            add_opportunity(item)
            if not is_new:
                continue
            new_count += 1
            result = score_opportunity(item)
            ranked_new.append((result.score, item))
            if args.notify and result.score >= args.min_score:
                send_alert(format_alert(item, result))
                notified += 1

        ranked_new.sort(key=lambda row: row[0], reverse=True)
        print(f"Fetched {len(projects)} Kwork projects; new={new_count}; notified={notified}")
        for score, item in ranked_new[:20]:
            budget = item.budget_rub if item.budget_rub is not None else "?"
            print(f"{score:>5} | {budget} RUB | {item.title} | {item.url}")
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
