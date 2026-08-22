from fom.models import Opportunity
from fom.scoring import score_opportunity


def test_short_relevant_task_scores_high():
    item = Opportunity(
        source="test",
        title="Python Telegram bot + Google Sheets",
        url="https://example.com/1",
        budget_rub=8000,
        estimated_hours=4,
        description="Automation via API and CSV",
    )
    result = score_opportunity(item)
    assert result.score >= 60
    assert not result.risk_flags


def test_low_value_long_task_is_penalized():
    item = Opportunity(
        source="test",
        title="Редактура большой книги",
        url="https://example.com/2",
        budget_rub=3000,
        estimated_hours=30,
        description="160000 знаков",
    )
    result = score_opportunity(item)
    assert result.score < 20


def test_scam_language_creates_risk_flags():
    item = Opportunity(
        source="test",
        title="Лёгкая работа без опыта 100000",
        url="https://example.com/3",
        budget_rub=10000,
        estimated_hours=2,
        description="Сначала оплатить доступ и внести депозит",
    )
    result = score_opportunity(item)
    assert result.risk_flags
    assert result.score < 30
