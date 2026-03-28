"""Aggregate raw records from all registered sources."""

from __future__ import annotations

import logging
from typing import Any

from scraper.sources import SOURCE_CLASSES
from scraper.sources.excel_manual_source import ExcelManualSource
from scraper.sources.multi_insurer_hospitals_source import MultiInsurerHospitalsSource

logger = logging.getLogger(__name__)

# Active chain (see scraper.sources.SOURCE_CLASSES): multi-insurer hospitals, schools stub, Excel manual.
assert MultiInsurerHospitalsSource in SOURCE_CLASSES, "Expected MultiInsurerHospitalsSource (replaces onefivenine)"
assert ExcelManualSource in SOURCE_CLASSES, "Expected Excel manual source after web scrapers"


def fetch_all_raw() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for cls in SOURCE_CLASSES:
        source = cls()
        key = source.source_key
        batch = source.fetch_raw()
        for row in batch:
            row = dict(row)
            # Per-row keys (e.g. insurer_star_health) from MultiInsurerHospitalsSource must stay intact.
            if "_source_key" not in row:
                row["_source_key"] = key
            rows.append(row)
        logger.info("fetch_raw: source=%s rows=%d", key, len(batch))
    logger.info("fetch_raw: total rows=%d", len(rows))
    return rows
