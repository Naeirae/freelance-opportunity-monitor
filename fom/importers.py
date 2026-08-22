from __future__ import annotations

import json
from pathlib import Path

from .db import add_opportunity
from .models import Opportunity


def import_json(path: str | Path) -> int:
    payload = json.loads(Path(path).read_text(encoding="utf-8"))
    if isinstance(payload, dict):
        payload = [payload]
    count = 0
    for row in payload:
        item = Opportunity(
            source=row["source"],
            title=row["title"],
            url=row["url"],
            budget_rub=row.get("budget_rub"),
            estimated_hours=row.get("estimated_hours"),
            description=row.get("description", ""),
            published_at=row.get("published_at"),
        )
        add_opportunity(item)
        count += 1
    return count
