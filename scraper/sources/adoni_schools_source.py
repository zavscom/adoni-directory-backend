"""School listings for Adoni — extend with real scrapers when URLs are available."""

from __future__ import annotations

import logging
from typing import Any

from scraper.sources.base_source import BaseSource

logger = logging.getLogger(__name__)


class AdoniSchoolsSource(BaseSource):
    """Placeholder: returns no rows until school scrapers are configured."""

    @property
    def source_key(self) -> str:
        return "schools_adoni"

    def fetch_raw(self) -> list[dict[str, Any]]:
        logger.info("schools_adoni: no scrapers configured (0 rows)")
        return []
