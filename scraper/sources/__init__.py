"""Scraper source modules registry."""

from scraper.sources.adoni_schools_source import AdoniSchoolsSource
from scraper.sources.base_source import BaseSource
from scraper.sources.dummy_source import DummySource
from scraper.sources.excel_manual_source import ExcelManualSource
from scraper.sources.multi_insurer_hospitals_source import MultiInsurerHospitalsSource
from scraper.sources.stub_source import MunicipalSiteStubSource

# Active sources: insurer hospitals, schools hook (may be empty), then Excel manual for overrides.
SOURCE_CLASSES: list[type[BaseSource]] = [
    MultiInsurerHospitalsSource,
    AdoniSchoolsSource,
    ExcelManualSource,
]

__all__ = [
    "BaseSource",
    "AdoniSchoolsSource",
    "DummySource",
    "ExcelManualSource",
    "MultiInsurerHospitalsSource",
    "MunicipalSiteStubSource",
    "SOURCE_CLASSES",
]
