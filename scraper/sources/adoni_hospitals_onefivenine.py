"""Scrape Adoni hospital listings from onefivenine.com town listing page."""

from __future__ import annotations

import logging
import re
from typing import Any
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup, NavigableString, Tag

from scraper.sources.base_source import BaseSource

logger = logging.getLogger(__name__)

LISTING_URL = "http://www.onefivenine.com/india/Listing/Town/hospitals/Kurnool/Adoni"
BASE_ORIGIN = "http://www.onefivenine.com"

DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
}


def _parse_listing_body(body: str) -> tuple[str | None, str | None, str]:
    """
    From text like '... India phone: 093932 38761 0.8 KM distance ...'
    return (phone_or_none, distance_km_str_or_none, address_line).
    """
    body = re.sub(r"\s+", " ", body).strip()
    body = re.sub(r"\s*Details\s*$", "", body, flags=re.I).strip()

    m_phone = re.search(r"\bphone\s*:", body, re.I)
    if m_phone:
        address = body[: m_phone.start()].strip().rstrip(",").strip()
        tail = body[m_phone.end() :].strip()
    else:
        address = body
        tail = ""

    dist_m = re.search(r"(\d+(?:\.\d+)?)\s*KM\s*distance", tail, re.I)
    distance_km: str | None = None
    phone_candidate = tail
    if dist_m:
        distance_km = dist_m.group(1)
        phone_candidate = tail[: dist_m.start()].strip()

    digits_in_candidate = re.sub(r"\D", "", phone_candidate)
    phone: str | None = None
    if len(digits_in_candidate) >= 6:
        phone = re.sub(r"\s+", "", phone_candidate).strip()

    return phone, distance_km, address


class AdoniHospitalsOneFiveNineSource(BaseSource):
    """Fetches hospital rows from the public onefivenine Adoni hospitals listing."""

    @property
    def source_key(self) -> str:
        return "onefivenine_hospitals"

    def fetch_raw(self) -> list[dict[str, Any]]:
        try:
            resp = requests.get(
                LISTING_URL,
                headers=DEFAULT_HEADERS,
                timeout=30,
            )
            resp.raise_for_status()
        except requests.RequestException as e:
            logger.exception("onefivenine: HTTP error for %s", LISTING_URL)
            raise RuntimeError(f"Failed to fetch listing: {e}") from e

        soup = BeautifulSoup(resp.text, "html.parser")
        rows: list[dict[str, Any]] = []

        for h2 in soup.find_all("h2"):
            name = h2.get_text(strip=True)
            if not name or _skip_heading(name):
                continue

            block_text, details_href = _collect_block_after_heading(h2)
            if not block_text:
                continue

            phone, distance_km, address = _parse_listing_body(block_text)
            extra: dict[str, Any] = {}
            if distance_km is not None:
                extra["distance_km"] = distance_km
            if details_href:
                extra["details_url"] = urljoin(BASE_ORIGIN, details_href)

            rows.append(
                {
                    "name": name,
                    "category": "Hospital",
                    "address": address or block_text,
                    "phone": phone,
                    "city": "Adoni",
                    "state": "Andhra Pradesh",
                    "extra": extra,
                }
            )

        logger.info("onefivenine_hospitals: parsed %d hospital rows", len(rows))
        return rows


def _skip_heading(title: str) -> bool:
    t = title.strip().lower()
    if t == "hospitals in adoni":
        return True
    if "all rights reserved" in t:
        return True
    return False


def _collect_block_after_heading(h2: Tag) -> tuple[str, str | None]:
    """Gather text and optional Details href from siblings until the next h2."""
    chunks: list[str] = []
    details_href: str | None = None
    node = h2.next_sibling
    while node is not None:
        if getattr(node, "name", None) == "h2":
            break
        if hasattr(node, "find_all") and hasattr(node, "get_text"):
            for a in node.find_all("a", href=True):
                href = str(a.get("href", ""))
                if "viewGGPlace" in href or "placeId" in href:
                    details_href = href
                    break
            t = node.get_text(" ", strip=True)
            if t:
                chunks.append(t)
        elif isinstance(node, NavigableString):
            s = str(node).strip()
            if s:
                chunks.append(s)
        node = node.next_sibling

    return " ".join(chunks), details_href
