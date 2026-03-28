"""Aggregate raw records from all registered sources."""

from __future__ import annotations

import logging
from typing import Any

from scraper.sources import SOURCE_CLASSES

logger = logging.getLogger(__name__)


def fetch_all_raw() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for cls in SOURCE_CLASSES:
        source = cls()
        key = source.source_key
        batch = source.fetch_raw()
        for row in batch:
            row = dict(row)
            row["_source_key"] = key
            rows.append(row)
        logger.info("fetch_raw: source=%s rows=%d", key, len(batch))
    logger.info("fetch_raw: total rows=%d", len(rows))
    return rows
