#!/usr/bin/env python3
"""Run fetch → normalize → dedupe → snapshot + delta for the configured town."""

from __future__ import annotations

import logging
import sys
from pathlib import Path

# Allow `python run_pipeline.py` from repo root without installing the package.
_REPO_ROOT = Path(__file__).resolve().parent
if str(_REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(_REPO_ROOT))

from scraper import config  # noqa: E402
from scraper.pipeline.dedupe import dedupe_businesses  # noqa: E402
from scraper.pipeline.fetch_raw import fetch_all_raw  # noqa: E402
from scraper.pipeline.generate_full_and_delta import run_full_and_delta  # noqa: E402
from scraper.pipeline.normalize import normalize_raw_records  # noqa: E402


def main() -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(levelname)s %(name)s: %(message)s",
    )
    log = logging.getLogger("run_pipeline")

    try:
        log.info("Town=%s state=%s", config.TOWN_NAME, config.STATE_NAME)
        log.info("Data dir=%s", config.DATA_DIR)

        raw = fetch_all_raw()
        normalized = normalize_raw_records(raw)
        unique = dedupe_businesses(normalized)
        summary = run_full_and_delta(unique)

        log.info(
            "Done. total=%d new=%d updated=%d deleted=%d",
            summary["total_count"],
            summary["new_count"],
            summary["updated_count"],
            summary["deleted_count"],
        )
        log.info("Full snapshot: %s", summary["full_path"])
        log.info("Delta: %s", summary["delta_path"])
    except Exception:
        log.exception("Pipeline failed")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
