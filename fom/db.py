from __future__ import annotations

import os
import sqlite3
from pathlib import Path

from .models import Opportunity


def db_path() -> Path:
    return Path(os.getenv("FOM_DB_PATH", "data/opportunities.db"))


def connect() -> sqlite3.Connection:
    path = db_path()
    path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    return conn


def init_db() -> None:
    with connect() as conn:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS opportunities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source TEXT NOT NULL,
                title TEXT NOT NULL,
                url TEXT NOT NULL UNIQUE,
                budget_rub INTEGER,
                estimated_hours REAL,
                description TEXT NOT NULL DEFAULT '',
                published_at TEXT,
                created_at TEXT NOT NULL
            )
            """
        )


def add_opportunity(item: Opportunity) -> int:
    init_db()
    with connect() as conn:
        cursor = conn.execute(
            """
            INSERT INTO opportunities
            (source, title, url, budget_rub, estimated_hours, description, published_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(url) DO UPDATE SET
                source=excluded.source,
                title=excluded.title,
                budget_rub=excluded.budget_rub,
                estimated_hours=excluded.estimated_hours,
                description=excluded.description,
                published_at=excluded.published_at
            """,
            (
                item.source,
                item.title,
                item.url,
                item.budget_rub,
                item.estimated_hours,
                item.description,
                item.published_at,
                item.created_at,
            ),
        )
        if cursor.lastrowid:
            return int(cursor.lastrowid)
        row = conn.execute("SELECT id FROM opportunities WHERE url = ?", (item.url,)).fetchone()
        return int(row["id"])


def list_opportunities() -> list[Opportunity]:
    init_db()
    with connect() as conn:
        rows = conn.execute("SELECT * FROM opportunities ORDER BY created_at DESC").fetchall()
    return [Opportunity(**dict(row)) for row in rows]
