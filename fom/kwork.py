from __future__ import annotations

import json
import os
from json import JSONDecoder
from typing import Iterable

import requests

from .models import Opportunity

KWORK_PROJECTS_URL = "https://kwork.ru/projects"
KWORK_PROJECT_URL = "https://kwork.ru/projects/{project_id}/view"
DEFAULT_TIMEOUT = 20


def _extract_state_data(html: str) -> dict:
    marker = "window.stateData"
    marker_pos = html.find(marker)
    if marker_pos < 0:
        raise ValueError("Kwork page does not contain window.stateData")

    equals_pos = html.find("=", marker_pos + len(marker))
    if equals_pos < 0:
        raise ValueError("Kwork stateData assignment is malformed")

    payload = html[equals_pos + 1 :].lstrip()
    data, _ = JSONDecoder().raw_decode(payload)
    if not isinstance(data, dict):
        raise ValueError("Kwork stateData is not an object")
    return data


def parse_projects_from_html(html: str) -> list[Opportunity]:
    data = _extract_state_data(html)
    raw_projects = data.get("wantsListData", {}).get("wants", [])
    projects: list[Opportunity] = []

    for raw in raw_projects:
        project_id = raw.get("id")
        title = str(raw.get("name") or "").strip()
        if not project_id or not title:
            continue

        price_raw = raw.get("priceLimit")
        try:
            budget = int(float(price_raw)) if price_raw not in (None, "") else None
        except (TypeError, ValueError):
            budget = None

        projects.append(
            Opportunity(
                source="kwork",
                title=title,
                url=KWORK_PROJECT_URL.format(project_id=project_id),
                budget_rub=budget,
                description=str(raw.get("description") or "").strip(),
                published_at=str(raw.get("dateCreate") or raw.get("created_at") or "").strip() or None,
            )
        )

    return projects


def _category_values(categories: Iterable[int] | None) -> list[int | None]:
    values = list(categories or [])
    return values if values else [None]


def fetch_kwork_projects(
    categories: Iterable[int] | None = None,
    *,
    page: int = 1,
    timeout: int = DEFAULT_TIMEOUT,
    session: requests.Session | None = None,
) -> list[Opportunity]:
    client = session or requests.Session()
    headers = {
        "User-Agent": "Mozilla/5.0 (compatible; FreelanceOpportunityMonitor/0.1; +https://github.com/Naeirae/freelance-opportunity-monitor)",
        "Accept-Language": "ru-RU,ru;q=0.9,en;q=0.7",
    }

    by_url: dict[str, Opportunity] = {}
    for category in _category_values(categories):
        params: dict[str, int] = {"page": page}
        if category is not None:
            params["c"] = category

        response = client.get(KWORK_PROJECTS_URL, params=params, headers=headers, timeout=timeout)
        response.raise_for_status()
        for item in parse_projects_from_html(response.text):
            by_url[item.url] = item

    return list(by_url.values())


def categories_from_env() -> list[int]:
    raw = os.getenv("KWORK_CATEGORIES", "").strip()
    if not raw:
        return []

    result: list[int] = []
    for token in raw.split(","):
        token = token.strip()
        if token:
            result.append(int(token))
    return result
