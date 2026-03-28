"""Base configuration and path constants."""

from pathlib import Path

# Repository root (parent of scraper package when imported from project root)
REPO_ROOT = Path(__file__).resolve().parent.parent

TOWN_NAME = "Adoni"
STATE_NAME = "Andhra Pradesh"

DATA_DIR = REPO_ROOT / "data"
FULL_SNAPSHOT_PATH = DATA_DIR / "adoni_full.json"
DELTA_PATH = DATA_DIR / "adoni_changes.json"

# ISO country context for addresses (optional metadata in extra later)
DEFAULT_CITY = TOWN_NAME
