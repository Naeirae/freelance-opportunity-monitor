from __future__ import annotations

import os

import requests

from .models import Opportunity
from .scoring import ScoreResult


def format_alert(item: Opportunity, result: ScoreResult) -> str:
    budget = f"{item.budget_rub:,} ₽".replace(",", " ") if item.budget_rub else "budget unknown"
    hours = f"~{item.estimated_hours:g} h" if item.estimated_hours else "time unknown"
    risk = ", ".join(result.risk_flags) if result.risk_flags else "none"
    return (
        f"{item.title}\n"
        f"{budget} · {hours} · score {result.score}\n"
        f"Source: {item.source}\n"
        f"Risks: {risk}\n"
        f"{item.url}"
    )


def send_alert(text: str) -> None:
    token = os.getenv("TELEGRAM_BOT_TOKEN")
    chat_id = os.getenv("TELEGRAM_CHAT_ID")
    if not token or not chat_id:
        raise RuntimeError("TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID are required")
    response = requests.post(
        f"https://api.telegram.org/bot{token}/sendMessage",
        json={"chat_id": chat_id, "text": text, "disable_web_page_preview": True},
        timeout=20,
    )
    response.raise_for_status()
