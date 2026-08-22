from __future__ import annotations

from dataclasses import dataclass

from .models import Opportunity


POSITIVE_KEYWORDS = {
    "python": 10,
    "excel": 8,
    "google sheets": 8,
    "csv": 6,
    "telegram": 8,
    "bot": 7,
    "api": 8,
    "automation": 10,
    "автоматизац": 10,
    "парсер": 7,
    "ai": 8,
    "ии": 8,
    "chatgpt": 8,
    "gemini": 7,
    "openai": 8,
    "редактор": 4,
    "коррект": 4,
}

RISK_KEYWORDS = {
    "оплатить доступ": 30,
    "страховой взнос": 30,
    "гарантийный взнос": 30,
    "внесите депозит": 30,
    "только telegram": 8,
    "ежедневный доход": 10,
    "без опыта 100000": 15,
}


@dataclass(slots=True)
class ScoreResult:
    score: float
    reasons: list[str]
    risk_flags: list[str]


def score_opportunity(item: Opportunity) -> ScoreResult:
    text = f"{item.title} {item.description}".lower()
    score = 0.0
    reasons: list[str] = []
    risks: list[str] = []

    if item.budget_rub:
        if item.budget_rub >= 10_000:
            score += 22
            reasons.append("budget >= 10k")
        elif item.budget_rub >= 5_000:
            score += 15
            reasons.append("budget >= 5k")
        elif item.budget_rub >= 3_000:
            score += 8
            reasons.append("budget >= 3k")
        else:
            score -= 8
            reasons.append("low budget")

    if item.hourly_rate:
        if item.hourly_rate >= 2_000:
            score += 25
            reasons.append("strong estimated hourly rate")
        elif item.hourly_rate >= 1_000:
            score += 15
            reasons.append("acceptable estimated hourly rate")
        elif item.hourly_rate < 500:
            score -= 15
            reasons.append("poor estimated hourly rate")

    if item.estimated_hours:
        if item.estimated_hours <= 4:
            score += 16
            reasons.append("can likely finish in a few hours")
        elif item.estimated_hours <= 10:
            score += 8
            reasons.append("short task")
        elif item.estimated_hours > 24:
            score -= 15
            reasons.append("long task")

    for keyword, weight in POSITIVE_KEYWORDS.items():
        if keyword in text:
            score += weight
            reasons.append(f"relevant: {keyword}")

    for keyword, penalty in RISK_KEYWORDS.items():
        if keyword in text:
            score -= penalty
            risks.append(keyword)

    return ScoreResult(round(score, 1), reasons, risks)
