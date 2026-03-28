"""Abstract base for pluggable scrape sources."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any


class BaseSource(ABC):
    """Each source returns provider-specific raw records (usually dicts)."""

    @property
    @abstractmethod
    def source_key(self) -> str:
        """Short identifier stored on Business.source (e.g. 'dummy', 'municipal_site')."""

    @abstractmethod
    def fetch_raw(self) -> list[dict[str, Any]]:
        """Return raw rows; shape is source-specific until normalize."""
