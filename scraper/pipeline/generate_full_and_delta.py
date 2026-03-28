"""Write full town snapshot and delta vs previous full snapshot."""

from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any

from scraper import config
from scraper.models import Business, utc_now_iso

logger = logging.getLogger(__name__)

FULL_WRAPPER_VERSION = 1


def load_previous_businesses(path: Path) -> dict[str, Business]:
    if not path.is_file():
        logger.info("generate: no previous snapshot at %s (treating as empty)", path)
        return {}
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        logger.warning("generate: invalid JSON in %s: %s (treating as empty)", path, e)
        return {}
    items = raw.get("businesses")
    if not isinstance(items, list):
        logger.warning("generate: missing businesses array in %s (treating as empty)", path)
        return {}
    out: dict[str, Business] = {}
    for item in items:
        if isinstance(item, dict) and "id" in item:
            b = Business.from_json_dict(item)
            out[b.id] = b
    logger.info("generate: loaded previous businesses=%d", len(out))
    return out


def compute_delta(
    previous: dict[str, Business],
    current: dict[str, Business],
) -> tuple[list[Business], list[Business], list[str]]:
    prev_ids = set(previous)
    cur_ids = set(current)
    new_ids = sorted(cur_ids - prev_ids)
    deleted_ids = sorted(prev_ids - cur_ids)
    new_rows = [current[i] for i in new_ids]
    deleted = deleted_ids

    updated: list[Business] = []
    for bid in sorted(cur_ids & prev_ids):
        if current[bid].fingerprint_for_delta() != previous[bid].fingerprint_for_delta():
            updated.append(current[bid])

    return new_rows, updated, deleted


def write_full_snapshot(path: Path, town: str, businesses: list[Business]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload: dict[str, Any] = {
        "version": FULL_WRAPPER_VERSION,
        "generatedAt": utc_now_iso(),
        "town": town,
        "businesses": [b.to_json_dict() for b in businesses],
    }
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    logger.info("generate: wrote full snapshot %s (businesses=%d)", path, len(businesses))


def write_delta_file(
    path: Path,
    town: str,
    new_rows: list[Business],
    updated: list[Business],
    deleted: list[str],
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "generatedAt": utc_now_iso(),
        "town": town,
        "new": [b.to_json_dict() for b in new_rows],
        "updated": [b.to_json_dict() for b in updated],
        "deleted": deleted,
    }
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    logger.info(
        "generate: wrote delta %s (new=%d updated=%d deleted=%d)",
        path,
        len(new_rows),
        len(updated),
        len(deleted),
    )


def run_full_and_delta(
    businesses: list[Business],
    full_path: Path | None = None,
    delta_path: Path | None = None,
    town: str | None = None,
) -> dict[str, Any]:
    full_path = full_path or config.FULL_SNAPSHOT_PATH
    delta_path = delta_path or config.DELTA_PATH
    town = town or config.TOWN_NAME

    previous = load_previous_businesses(full_path)
    current = {b.id: b for b in businesses}
    new_rows, updated, deleted = compute_delta(previous, current)

    write_full_snapshot(full_path, town, businesses)
    write_delta_file(delta_path, town, new_rows, updated, deleted)

    return {
        "full_path": str(full_path),
        "delta_path": str(delta_path),
        "new_count": len(new_rows),
        "updated_count": len(updated),
        "deleted_count": len(deleted),
        "total_count": len(businesses),
    }
