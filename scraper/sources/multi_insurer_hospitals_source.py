"""Scrape cashless network hospitals from PolicyX, InsuranceDekho, and ICICI Lombard pages."""

from __future__ import annotations

import logging
import re
import time
from typing import Any

import requests
from bs4 import BeautifulSoup, Tag

from scraper import config
from scraper.models import normalize_name_key
from scraper.sources.base_source import BaseSource

logger = logging.getLogger(__name__)

REQUEST_DELAY_SEC = 2.0
REQUEST_TIMEOUT = 45

DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
}

_PINCODE_RE = re.compile(r"\b(\d{6})\b")
_PHONE_RE = re.compile(r"[\d\s\-+().]{8,}")

# (insurer_slug, url) — Business.source / _source_key becomes f"insurer_{slug}" (e.g. insurer_star_health).
INSURER_URLS: list[tuple[str, str]] = [
    (
        "star_health",
        "https://www.policyx.com/health-insurance/network-hospitals/star-health-adoni-andhra-pradesh/",
    ),
    (
        "hdfc_ergo",
        "https://www.insurancedekho.com/health-insurance/hdfc-ergo/network-hospitals-in-adoni",
    ),
    (
        "icici_lombard",
        "https://www.icicilombard.com/blogs/health-insurance/mb/list-of-cashless-network-hospitals-in-kurnool",
    ),
]


def _insurer_source(insurer_slug: str) -> str:
    return f"insurer_{insurer_slug}"


def _fetch_html(url: str) -> str:
    headers = dict(DEFAULT_HEADERS)
    if "icicilombard.com" in url:
        headers["Referer"] = "https://www.icicilombard.com/"
        headers["Accept"] = (
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
        )
    r = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
    if r.status_code == 403 and "icicilombard.com" in url:
        # Site often blocks datacenter/bot requests; Jina reader returns page text.
        mirror = f"https://r.jina.ai/{url}"
        logger.info("multi_insurer: using reader mirror for ICICI Lombard")
        try:
            r2 = requests.get(mirror, headers=DEFAULT_HEADERS, timeout=REQUEST_TIMEOUT)
            r2.raise_for_status()
            return r2.text
        except Exception as e:
            logger.warning("multi_insurer: ICICI reader mirror failed: %s", e)
            return ""
    r.raise_for_status()
    return r.text


def _clean_phone(text: str | None) -> str | None:
    if not text or not str(text).strip() or str(text).strip() in {"-", "—", "NA", "N/A"}:
        return None
    s = re.sub(r"\s+", " ", str(text).strip())
    digits = re.sub(r"\D", "", s)
    return s if len(digits) >= 6 else None


def _pin_from_text(*parts: str) -> str:
    for p in parts:
        if not p:
            continue
        m = _PINCODE_RE.search(p)
        if m:
            return m.group(1)
    return ""


def _row_dict(
    *,
    name: str,
    address: str,
    city: str,
    state: str,
    pincode: str,
    phone: str | None,
    row_source: str,
) -> dict[str, Any]:
    name = name.strip()
    address = address.strip()
    if not name:
        return {}
    return {
        "name": name,
        "category": "Hospital",
        "address": address,
        "area": "",
        "pincode": pincode or _pin_from_text(address),
        "city": city.strip() or config.DEFAULT_CITY,
        "state": state.strip() or config.STATE_NAME,
        "phone": phone,
        "whatsapp": None,
        "_source_key": row_source,
    }


def _parse_policyx_star(html: str, insurer_source: str) -> list[dict[str, Any]]:
    """Star Health Adoni table on PolicyX."""
    soup = BeautifulSoup(html, "html.parser")
    out: list[dict[str, Any]] = []
    for table in soup.find_all("table"):
        rows = table.find_all("tr")
        if len(rows) < 2:
            continue
        header_cells = rows[0].find_all(["th", "td"])
        headers = [c.get_text(strip=True).lower() for c in header_cells]
        joined = " ".join(headers)
        if "hospital" not in joined or "name" not in joined:
            continue
        col_map: dict[str, int] = {}
        for i, h in enumerate(headers):
            if "hospital" in h and "name" in h:
                col_map["name"] = i
            elif h == "address" or "address" in h:
                col_map["address"] = i
            elif "pin" in h:
                col_map["pin"] = i
            elif "city" in h:
                col_map["city"] = i
            elif "state" in h:
                col_map["state"] = i
            elif "contact" in h:
                col_map["phone"] = i
        if "name" not in col_map or "address" not in col_map:
            continue

        for tr in rows[1:]:
            cells = tr.find_all(["td", "th"])
            if len(cells) <= max(col_map.values()):
                continue
            def txt(k: str) -> str:
                i = col_map.get(k)
                if i is None or i >= len(cells):
                    return ""
                return cells[i].get_text(" ", strip=True)

            name = txt("name")
            address = txt("address")
            if not name or not address:
                continue
            pin = txt("pin") or _pin_from_text(address)
            city = txt("city") or config.DEFAULT_CITY
            state = txt("state") or config.STATE_NAME
            phone = _clean_phone(txt("phone"))
            d = _row_dict(
                name=name,
                address=address,
                city=city,
                state=state,
                pincode=pin,
                phone=phone,
                row_source=insurer_source,
            )
            if d:
                out.append(d)
        if out:
            break
    return out


def _parse_insurancedekho_hdfc(html: str, insurer_source: str) -> list[dict[str, Any]]:
    """HDFC ERGO Adoni — table with Hospital Name | Address."""
    soup = BeautifulSoup(html, "html.parser")
    out: list[dict[str, Any]] = []
    for table in soup.find_all("table"):
        rows = table.find_all("tr")
        if len(rows) < 2:
            continue
        h0 = [c.get_text(strip=True).lower() for c in rows[0].find_all(["th", "td"])]
        if len(h0) < 2:
            continue
        if "hospital" not in h0[0] and "name" not in h0[0]:
            continue
        if "address" not in h0[1]:
            continue
        for tr in rows[1:]:
            cells = tr.find_all(["td", "th"])
            if len(cells) < 2:
                continue
            name = cells[0].get_text(" ", strip=True)
            address = cells[1].get_text(" ", strip=True)
            if not name or not address:
                continue
            if "get direction" in name.lower() or "search" in name.lower():
                continue
            pin = _pin_from_text(address)
            d = _row_dict(
                name=name,
                address=address,
                city=config.DEFAULT_CITY,
                state=config.STATE_NAME,
                pincode=pin,
                phone=None,
                row_source=insurer_source,
            )
            if d:
                out.append(d)
        if out:
            break
    return out


def _icici_row_matches_adoni_area(blob: str) -> bool:
    """Kurnool district list: Adoni town rows often use pin 518301 or mention Adoni."""
    low = blob.lower()
    if "adoni" in low:
        return True
    if "518301" in blob:
        return True
    return False


def _parse_icici_pipe_markdown(text: str, insurer_source: str) -> list[dict[str, Any]]:
    """Jina / markdown-style pipe rows (no HTML tables)."""
    if "Markdown Content:" in text:
        text = text.split("Markdown Content:", 1)[1]
    out: list[dict[str, Any]] = []
    seen: set[tuple[str, str]] = set()

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line.startswith("|"):
            continue
        if re.match(r"^\|\s*-+", line):
            continue
        inner = [p.strip() for p in line.strip("|").split("|")]
        parts = [p for p in inner if p]
        if len(parts) < 2:
            continue
        blob = " | ".join(parts)
        if not _icici_row_matches_adoni_area(blob):
            continue
        name = parts[0]
        address = parts[1] if len(parts) > 1 else ""
        if name.lower() in {"hospital name", "name", "s.no", "sr no", "hospital"}:
            continue
        if not name or len(name) < 2:
            continue
        key = (normalize_name_key(name), address[:120].lower())
        if key in seen:
            continue
        seen.add(key)
        pin = _pin_from_text(blob)
        phone = None
        for t in parts:
            m = _PHONE_RE.search(t)
            if m and len(re.sub(r"\D", "", m.group(0))) >= 8:
                phone = _clean_phone(m.group(0))
                break
        d = _row_dict(
            name=name,
            address=address or blob,
            city=config.DEFAULT_CITY,
            state=config.STATE_NAME,
            pincode=pin,
            phone=phone,
            row_source=insurer_source,
        )
        if d:
            out.append(d)
    return out


def _parse_icici_kurnool_adoni(html: str, insurer_source: str) -> list[dict[str, Any]]:
    """ICICI Lombard Kurnool list — Adoni area (name/address/pin heuristics)."""
    soup = BeautifulSoup(html, "html.parser")
    out: list[dict[str, Any]] = []
    seen: set[tuple[str, str]] = set()

    for table in soup.find_all("table"):
        for tr in table.find_all("tr"):
            if not isinstance(tr, Tag):
                continue
            cells = tr.find_all(["td", "th"])
            if len(cells) < 2:
                continue
            texts = [c.get_text(" ", strip=True) for c in cells]
            blob = " | ".join(texts)
            if not _icici_row_matches_adoni_area(blob):
                continue
            name = texts[0]
            address = texts[1] if len(texts) > 1 else ""
            if len(texts) >= 3 and len(texts[0]) < 3:
                name = texts[1]
                address = texts[2] if len(texts) > 2 else blob
            if not name or len(name) < 2:
                continue
            if name.lower() in {"hospital name", "name", "s.no", "sr no", "location"}:
                continue
            key = (normalize_name_key(name), address[:120].lower())
            if key in seen:
                continue
            seen.add(key)
            pin = _pin_from_text(address, *texts)
            phone = None
            for t in texts:
                m = _PHONE_RE.search(t)
                if m and len(re.sub(r"\D", "", m.group(0))) >= 8:
                    phone = _clean_phone(m.group(0))
                    break
            d = _row_dict(
                name=name,
                address=address or blob,
                city=config.DEFAULT_CITY,
                state=config.STATE_NAME,
                pincode=pin,
                phone=phone,
                row_source=insurer_source,
            )
            if d:
                out.append(d)

    if out:
        return out
    return _parse_icici_pipe_markdown(html, insurer_source)


def _dedupe_records(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Drop near-duplicates (same normalized name + address prefix)."""
    best: dict[tuple[str, str], dict[str, Any]] = {}
    for r in records:
        nk = normalize_name_key(r["name"])
        addr_key = (r.get("address") or "")[:100].strip().lower()
        k = (nk, addr_key)
        if k not in best or len(r.get("address", "")) > len(best[k].get("address", "")):
            best[k] = r
    return list(best.values())


class MultiInsurerHospitalsSource(BaseSource):
    """
    Scrapes Star (PolicyX), HDFC ERGO (InsuranceDekho), and ICICI Lombard (Kurnool list → Adoni-area rows).

    Replaces the legacy onefivenine.com town listing. Each row sets ``_source_key`` to
    ``insurer_<slug>`` (e.g. ``insurer_star_health``) for ``normalize`` / JSON ``source``.
    """

    @property
    def source_key(self) -> str:
        return "multi_insurer_hospitals"

    def fetch_raw(self) -> list[dict[str, Any]]:
        combined: list[dict[str, Any]] = []

        parsers: dict[str, Any] = {
            "star_health": _parse_policyx_star,
            "hdfc_ergo": _parse_insurancedekho_hdfc,
            "icici_lombard": _parse_icici_kurnool_adoni,
        }

        for i, (insurer_slug, url) in enumerate(INSURER_URLS):
            if i > 0:
                time.sleep(REQUEST_DELAY_SEC)
            row_source = _insurer_source(insurer_slug)
            parser = parsers[insurer_slug]
            try:
                html = _fetch_html(url)
                batch = parser(html, row_source)
                logger.info(
                    "multi_insurer: insurer=%s url=%s rows=%d",
                    row_source,
                    url,
                    len(batch),
                )
                combined.extend(batch)
            except Exception as e:
                logger.warning("multi_insurer: skipped %s: %s", url, e)

        if not combined:
            raise RuntimeError(
                "Multi-insurer source: no rows from any URL (check network or page markup)."
            )

        deduped = _dedupe_records(combined)
        logger.info(
            "multi_insurer: found %d insurer hospitals (raw=%d before dedupe)",
            len(deduped),
            len(combined),
        )
        return deduped
