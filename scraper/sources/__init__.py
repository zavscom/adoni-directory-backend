"""Scraper source modules registry."""

from scraper.sources.base_source import BaseSource
from scraper.sources.dummy_source import DummySource
from scraper.sources.stub_source import MunicipalSiteStubSource

# Add new sources here for fetch_raw to discover them.
SOURCE_CLASSES: list[type[BaseSource]] = [
    DummySource,
    # MunicipalSiteStubSource,  # enable when implemented (currently returns [])
]

__all__ = ["BaseSource", "DummySource", "MunicipalSiteStubSource", "SOURCE_CLASSES"]
