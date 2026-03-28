"""Business domain model, serialization, and stable ID helpers."""

from __future__ import annotations

import hashlib
import json
import re
from dataclasses import asdict, dataclass, field
from datetime import datetime, timezone
from typing import Any, Mapping, Optional


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def normalize_phone(value: Optional[str]) -> Optional[str]:
    if value is None:
        return None
    digits = re.sub(r"\D", "", value.strip())
    # Collapse common India formats: 91 + 10-digit mobile
    if len(digits) == 12 and digits.startswith("91"):
        digits = digits[2:]
    if len(digits) == 11 and digits.startswith("0"):
        digits = digits[1:]
    return digits or None


def normalize_name_key(name: str) -> str:
    return re.sub(r"\s+", " ", (name or "").strip().lower())


def stable_business_id(name: str, phone: Optional[str], pincode: str) -> str:
    """Deterministic id from dedupe key: normalized name + phone + pincode."""
    key = f"{normalize_name_key(name)}|{normalize_phone(phone) or ''}|{(pincode or '').strip()}"
    return hashlib.sha256(key.encode("utf-8")).hexdigest()[:32]


@dataclass
class Business:
    id: str
    name: str
    category: str
    subCategory: Optional[str]
    address: str
    area: str
    pincode: str
    city: str
    state: str
    phone: Optional[str]
    whatsapp: Optional[str]
    email: Optional[str]
    website: Optional[str]
    latitude: Optional[float]
    longitude: Optional[float]
    source: str
    lastSeenAt: str
    extra: dict[str, Any] = field(default_factory=dict)

    def to_json_dict(self) -> dict[str, Any]:
        d = asdict(self)
        # JSON nulls for optional scalar fields
        for k in (
            "subCategory",
            "phone",
            "whatsapp",
            "email",
            "website",
            "latitude",
            "longitude",
        ):
            if d.get(k) is None:
                d[k] = None
        return d

    @classmethod
    def from_json_dict(cls, data: Mapping[str, Any]) -> Business:
        return cls(
            id=str(data["id"]),
            name=str(data["name"]),
            category=str(data["category"]),
            subCategory=data.get("subCategory"),
            address=str(data.get("address", "")),
            area=str(data.get("area", "")),
            pincode=str(data.get("pincode", "")),
            city=str(data.get("city", "")),
            state=str(data.get("state", "")),
            phone=data.get("phone"),
            whatsapp=data.get("whatsapp"),
            email=data.get("email"),
            website=data.get("website"),
            latitude=_optional_float(data.get("latitude")),
            longitude=_optional_float(data.get("longitude")),
            source=str(data.get("source", "")),
            lastSeenAt=str(data.get("lastSeenAt", "")),
            extra=dict(data.get("extra") or {}),
        )

    def fingerprint_for_delta(self) -> str:
        """Stable string for detecting substantive changes (excludes lastSeenAt)."""
        d = self.to_json_dict()
        d.pop("lastSeenAt", None)
        return json.dumps(d, sort_keys=True, ensure_ascii=False)


def _optional_float(v: Any) -> Optional[float]:
    if v is None:
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None
