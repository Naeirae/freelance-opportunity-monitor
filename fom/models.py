from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone


@dataclass(slots=True)
class Opportunity:
    source: str
    title: str
    url: str
    budget_rub: int | None = None
    estimated_hours: float | None = None
    description: str = ""
    published_at: str | None = None
    id: int | None = None
    created_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    @property
    def hourly_rate(self) -> float | None:
        if not self.budget_rub or not self.estimated_hours or self.estimated_hours <= 0:
            return None
        return self.budget_rub / self.estimated_hours
