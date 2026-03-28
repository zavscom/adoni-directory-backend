"""Hardcoded sample businesses for pipeline testing."""

from __future__ import annotations

from typing import Any

from scraper import config
from scraper.sources.base_source import BaseSource


class DummySource(BaseSource):
    @property
    def source_key(self) -> str:
        return "dummy"

    def fetch_raw(self) -> list[dict[str, Any]]:
        town = config.TOWN_NAME
        state = config.STATE_NAME
        return [
            {
                "name": "Adoni Grand Hotel",
                "category": "hotels",
                "subCategory": "budget",
                "address": "Station Road, near Bus Stand",
                "area": "Bus Stand Area",
                "pincode": "518301",
                "city": town,
                "state": state,
                "phone": "08512-123456",
                "whatsapp": None,
                "email": "frontdesk@example.invalid",
                "website": None,
                "latitude": 15.6280,
                "longitude": 77.2749,
            },
            {
                "name": "Spice Route Restaurant",
                "category": "restaurants",
                "subCategory": "north_indian",
                "address": "MG Road",
                "area": "MG Road",
                "pincode": "518301",
                "city": town,
                "state": state,
                "phone": "9876543210",
                "whatsapp": "919876543210",
                "email": None,
                "website": "https://example.invalid/spiceroute",
                "latitude": None,
                "longitude": None,
            },
            {
                "name": "City Care Clinic",
                "category": "clinics",
                "subCategory": "general",
                "address": "Hospital Road",
                "area": "Civil Hospital Road",
                "pincode": "518301",
                "city": town,
                "state": state,
                "phone": "08512-999888",
                "whatsapp": None,
                "email": None,
                "website": None,
                "latitude": None,
                "longitude": None,
            },
            # Intentional near-duplicate for dedupe testing (same name/phone/pincode as Spice Route)
            {
                "name": "Spice Route Restaurant",
                "category": "restaurants",
                "subCategory": None,
                "address": "MG Road, 1st Floor",
                "area": "MG Road",
                "pincode": "518301",
                "city": town,
                "state": state,
                "phone": "+91 98765 43210",
                "whatsapp": None,
                "email": "info@example.invalid",
                "website": None,
                "latitude": None,
                "longitude": None,
            },
        ]
