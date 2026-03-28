"""Merge duplicate businesses by normalized name + phone + pincode (same id)."""

from __future__ import annotations

import logging
from typing import Iterable

from scraper.models import Business

logger = logging.getLogger(__name__)


def dedupe_businesses(businesses: Iterable[Business]) -> list[Business]:
    """Keep one row per id; merge fields from duplicates."""
    rows = list(businesses)
    by_id: dict[str, Business] = {}
    for b in rows:
        existing = by_id.get(b.id)
        if existing is None:
            by_id[b.id] = b
        else:
            by_id[b.id] = _merge_two(existing, b)
    out = list(by_id.values())
    merged = len(rows) - len(out)
    if merged:
        logger.info("dedupe: merged duplicate groups, removed %d duplicate rows", merged)
    logger.info("dedupe: unique businesses=%d", len(out))
    return out


def _merge_two(a: Business, b: Business) -> Business:
    """Prefer non-empty values; combine sources; latest lastSeenAt."""

    def pick_str(x: str | None, y: str | None) -> str | None:
        if x and (not y or len(x) >= len(y or "")):
            return x
        return y if y else x

    def pick_float(x: float | None, y: float | None) -> float | None:
        return x if x is not None else y

    sources = {s.strip() for s in (a.source + "," + b.source).split(",") if s.strip()}
    merged_source = ",".join(sorted(sources))
    extra = {**a.extra, **b.extra}

    last_seen = max(a.lastSeenAt, b.lastSeenAt, key=_iso_sort_key)

    return Business(
        id=a.id,
        name=a.name if len(a.name) >= len(b.name) else b.name,
        category=a.category or b.category,
        subCategory=pick_str(a.subCategory, b.subCategory),
        address=a.address if len(a.address) >= len(b.address) else b.address,
        area=a.area if len(a.area) >= len(b.area) else b.area,
        pincode=a.pincode or b.pincode,
        city=a.city or b.city,
        state=a.state or b.state,
        phone=pick_str(a.phone, b.phone),
        whatsapp=pick_str(a.whatsapp, b.whatsapp),
        email=pick_str(a.email, b.email),
        website=pick_str(a.website, b.website),
        latitude=pick_float(a.latitude, b.latitude),
        longitude=pick_float(a.longitude, b.longitude),
        source=merged_source,
        lastSeenAt=last_seen,
        extra=extra,
    )


def _iso_sort_key(s: str) -> str:
    """Lexicographic ordering is fine for ISO8601 UTC with Z suffix."""
    return s
