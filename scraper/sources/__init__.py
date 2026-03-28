"""Scraper source modules registry."""

from scraper.sources.adoni_hospitals_onefivenine import AdoniHospitalsOneFiveNineSource
from scraper.sources.base_source import BaseSource
from scraper.sources.dummy_source import DummySource
from scraper.sources.stub_source import MunicipalSiteStubSource

# Active sources for fetch_raw (dummy disabled — use real listings).
SOURCE_CLASSES: list[type[BaseSource]] = [
    AdoniHospitalsOneFiveNineSource,
]

__all__ = [
    "BaseSource",
    "AdoniHospitalsOneFiveNineSource",
    "DummySource",
    "MunicipalSiteStubSource",
    "SOURCE_CLASSES",
]
