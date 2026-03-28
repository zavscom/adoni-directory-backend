"""Map raw source rows to Business instances."""

from __future__ import annotations

import logging
import re
from typing import Any, Mapping

from scraper import config
from scraper.models import Business, stable_business_id, utc_now_iso

logger = logging.getLogger(__name__)

_PINCODE_RE = re.compile(r"\b(\d{6})\b")


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
    address = str(raw.get("address", "")).strip()
    explicit_id = raw.get("id")
    if explicit_id is not None and str(explicit_id).strip():
        bid = str(explicit_id).strip()
    else:
        bid = stable_business_id(source_key, name, address)
    now = utc_now_iso()

    pincode = _extract_pincode(address, raw.get("pincode"))
    area = str(raw.get("area", "")).strip() or _derive_area_from_address(address)
    phone = _optional_str(raw.get("phone"))

    extra = dict(raw.get("extra") or {})

    return Business(
        id=bid,
        name=name,
        category=str(raw.get("category", "uncategorized")).strip(),
        subCategory=_optional_str(raw.get("subCategory")),
        address=address,
        area=area,
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
        extra=extra,
    )


def _extract_pincode(address: str, raw_pincode: Any) -> str:
    if raw_pincode is not None and str(raw_pincode).strip():
        return str(raw_pincode).strip()
    m = _PINCODE_RE.search(address or "")
    return m.group(1) if m else ""


def _derive_area_from_address(address: str) -> str:
    """Use segments before the one containing 'Adoni', else first ';'-separated segment."""
    if not address.strip():
        return ""
    parts = [p.strip() for p in address.split(";") if p.strip()]
    if not parts:
        return ""
    idx = next((i for i, p in enumerate(parts) if re.search(r"\badoni\b", p, re.I)), None)
    if idx is not None and idx > 0:
        return "; ".join(parts[:idx])
    return parts[0]


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
