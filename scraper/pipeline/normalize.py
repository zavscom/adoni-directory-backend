"""Map raw source rows to Business instances."""

from __future__ import annotations

import logging
from typing import Any, Mapping

from scraper import config
from scraper.models import Business, stable_business_id, utc_now_iso

logger = logging.getLogger(__name__)


def normalize_raw_records(raw_rows: list[dict[str, Any]]) -> list[Business]:
    businesses: list[Business] = []
    for raw in raw_rows:
        source_key = str(raw.pop("_source_key", "unknown"))
        b = _row_to_business(raw, source_key)
        businesses.append(b)
    logger.info("normalize: output businesses=%d", len(businesses))
    return businesses


def _row_to_business(raw: Mapping[str, Any], source_key: str) -> Business:
    name = str(raw.get("name", "")).strip()
    phone = _optional_str(raw.get("phone"))
    pincode = str(raw.get("pincode", "")).strip()
    bid = stable_business_id(name, phone, pincode)
    now = utc_now_iso()
    return Business(
        id=bid,
        name=name,
        category=str(raw.get("category", "uncategorized")).strip(),
        subCategory=_optional_str(raw.get("subCategory")),
        address=str(raw.get("address", "")).strip(),
        area=str(raw.get("area", "")).strip(),
        pincode=pincode,
        city=str(raw.get("city", config.DEFAULT_CITY)).strip(),
        state=str(raw.get("state", config.STATE_NAME)).strip(),
        phone=phone,
        whatsapp=_optional_str(raw.get("whatsapp")),
        email=_optional_str(raw.get("email")),
        website=_optional_str(raw.get("website")),
        latitude=_optional_float(raw.get("latitude")),
        longitude=_optional_float(raw.get("longitude")),
        source=source_key,
        lastSeenAt=now,
        extra=dict(raw.get("extra") or {}),
    )


def _optional_str(v: Any) -> str | None:
    if v is None:
        return None
    s = str(v).strip()
    return s or None


def _optional_float(v: Any) -> float | None:
    if v is None or v == "":
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None
