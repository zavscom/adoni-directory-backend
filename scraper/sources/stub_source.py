"""Template for future sources: returns no rows until implemented."""

from __future__ import annotations

from typing import Any

from scraper.sources.base_source import BaseSource


class MunicipalSiteStubSource(BaseSource):
    """Placeholder; implement fetch_raw with real HTTP + parse logic later."""

    @property
    def source_key(self) -> str:
        return "municipal_site"

    def fetch_raw(self) -> list[dict[str, Any]]:
        return []
