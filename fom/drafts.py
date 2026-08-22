from __future__ import annotations

from .models import Opportunity
from .scoring import ScoreResult


def suggested_price(item: Opportunity) -> int | None:
    """Return a conservative suggested bid for short freelance tasks."""
    if item.budget_rub is not None:
        return item.budget_rub
    if item.estimated_hours is None:
        return None
    # Target at least 1,500 RUB/hour for small custom automation work,
    # with a 3,000 RUB minimum project price.
    return max(3_000, int(round(item.estimated_hours * 1_500 / 500) * 500))


def build_application_draft(item: Opportunity, result: ScoreResult) -> str:
    price = suggested_price(item)
    price_line = f" Предлагаемая стоимость: {price:,} ₽.".replace(",", " ") if price else ""

    capabilities: list[str] = []
    text = f"{item.title} {item.description}".lower()
    if any(k in text for k in ("python", "скрипт", "парсер", "automation", "автоматизац")):
        capabilities.append("Python и автоматизация")
    if any(k in text for k in ("excel", "google sheets", "csv", "таблиц")):
        capabilities.append("Excel/Google Sheets и обработка данных")
    if any(k in text for k in ("telegram", "бот", "bot")):
        capabilities.append("Telegram-боты")
    if any(k in text for k in ("ai", "ии", "chatgpt", "openai", "gemini")):
        capabilities.append("AI-интеграции")

    skill_line = ", ".join(capabilities) if capabilities else "автоматизация и работа с данными"
    risk_note = "" if not result.risk_flags else " Перед стартом уточню детали и формат безопасной передачи доступов."

    return (
        f"Здравствуйте! Могу взять задачу в работу. Работаю с {skill_line}; "
        "сначала уточняю входные данные и критерий готовности, затем отдаю рабочий результат "
        "с короткой инструкцией по запуску."
        f"{price_line}{risk_note} Если пришлёте пример входных данных/ожидаемого результата, "
        "быстро подтвержу срок."
    )
