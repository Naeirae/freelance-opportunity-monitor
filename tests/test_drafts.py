from fom.drafts import build_application_draft, suggested_price
from fom.models import Opportunity
from fom.scoring import score_opportunity


def test_suggested_price_uses_budget_when_known():
    item = Opportunity(source="test", title="Task", url="https://example.com", budget_rub=7000)
    assert suggested_price(item) == 7000


def test_suggested_price_has_minimum_for_small_unknown_budget_task():
    item = Opportunity(source="test", title="Task", url="https://example.com", estimated_hours=1)
    assert suggested_price(item) == 3000


def test_draft_mentions_relevant_capabilities():
    item = Opportunity(
        source="test",
        title="Telegram bot with AI",
        url="https://example.com",
        budget_rub=6000,
        estimated_hours=3,
        description="Python OpenAI API integration",
    )
    result = score_opportunity(item)
    draft = build_application_draft(item, result)
    assert "Python" in draft
    assert "Telegram" in draft
    assert "AI" in draft
