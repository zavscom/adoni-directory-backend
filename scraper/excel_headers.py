"""Shared Excel column names for manual import/export."""

from __future__ import annotations

# Order used in export sheets and for aligning preserved rows.
EXCEL_BUSINESS_HEADERS: list[str] = [
    "id",
    "name",
    "category",
    "subCategory",
    "address",
    "area",
    "pincode",
    "phone",
    "whatsapp",
    "email",
    "website",
    "city",
    "state",
    "latitude",
    "longitude",
    "source",
    "lastSeenAt",
    "extra_json",
]

_KNOWN: frozenset[str] = frozenset(EXCEL_BUSINESS_HEADERS)

_ALIASES: dict[str, str] = {
    "subcategory": "subCategory",
    "sub_category": "subCategory",
    "sub category": "subCategory",
    "zip": "pincode",
    "zipcode": "pincode",
    "postalcode": "pincode",
    "postal code": "pincode",
    "extra": "extra_json",
    "extras": "extra_json",
}


def canonical_excel_header(cell: object | None) -> str | None:
    """Map a sheet header cell to a key in EXCEL_BUSINESS_HEADERS."""
    if cell is None:
        return None
    s = str(cell).strip()
    if not s:
        return None
    key = s.lower().replace("_", " ").strip()
    if key in _ALIASES:
        return _ALIASES[key]
    compact = key.replace(" ", "")
    if compact in _ALIASES:
        return _ALIASES[compact]
    for h in EXCEL_BUSINESS_HEADERS:
        if h.lower().replace("_", "") == compact:
            return h
    return None


def is_known_excel_field(name: str) -> bool:
    return name in _KNOWN
