# Adoni business directory scraper

Python 3 pipeline that collects public business listings (one town first), normalizes them to a shared schema, deduplicates, and writes a full JSON snapshot plus a delta file for Git-friendly storage.

**GitHub repository:** [zavscom/adoni-directory-backend](https://github.com/zavscom/adoni-directory-backend)

## Setup

```bash
cd /path/to/adoniinfo
python -m venv .venv
.venv\Scripts\activate   # Windows
pip install -r requirements.txt
```

## Run

From the repository root:

```bash
python run_pipeline.py
```

Logs show counts per stage. Outputs:

- `data/adoni_full.json` — full snapshot (`town`, `generatedAt`, `businesses[]`)
- `data/adoni_changes.json` — delta: `new`, `updated`, `deleted` since the previous full snapshot

## Layout

| Path | Role |
|------|------|
| `scraper/config.py` | Town name, paths |
| `scraper/models.py` | `Business` dataclass, stable `id`, helpers |
| `scraper/schema/business.json` | JSON Schema for one business object |
| `scraper/sources/` | Pluggable sources (`adoni_hospitals_onefivenine.py`, `dummy_source.py` optional) |
| `scraper/pipeline/` | `fetch_raw` → `normalize` → `dedupe` → `generate_full_and_delta` |

Add a new source: implement `BaseSource`, register the class in `scraper/sources/__init__.py` (`SOURCE_CLASSES`).

## Business model

Stable `id` is **SHA-1** (hex) of **source + normalized name + normalized address**. Duplicates with the same id are merged in `dedupe.py`. Phone normalization strips non-digits and collapses common India patterns (e.g. leading `91` on a 12-digit mobile) when present on the entity.

Delta rules: **new** / **deleted** by id; **updated** when any field except `lastSeenAt` changes compared to the previous full snapshot.

## GitHub as backend

Commit `data/*.json` after each run; deltas keep diffs smaller than replacing only the full file, depending on churn.

**First push from this folder** (if the remote already has a `README.md`, pull or rebase first, then push):

```bash
git init
git remote add origin https://github.com/zavscom/adoni-directory-backend.git
git branch -M main
git add .
git commit -m "Initial import: scraper, data, Actions, Pages"
git push -u origin main
```

If GitHub already created a commit (e.g. `README.md` on the remote), run `git pull origin main --rebase` before the first `git push`, or use `--allow-unrelated-histories` once if both sides have initial commits.

## GitHub Actions

Workflow **scrape-town-data** (`.github/workflows/scrape-town-data.yml`):

- **Schedule:** daily at **03:00 IST** (`cron` is **21:30 UTC**; India has no daylight saving).
- **Manual:** Actions → scrape-town-data → Run workflow.

The job installs dependencies, runs `python run_pipeline.py`, and **commits and pushes** any changes under `data/` with message `Update town data <UTC ISO timestamp>`. Uses `GITHUB_TOKEN` with `contents: write`.

If your default branch is not `main`, update the workflow checkout/push target or set the repository default branch to `main`.

## GitHub Pages and Android endpoints

Enable **GitHub Pages** on this repo (e.g. **Deploy from a branch**, folder **/** root on **main**). A `.nojekyll` file at the repo root disables Jekyll so static files under `data/` are served as-is.

With a **project site** for this repo, the Android app can use:

| File | URL |
|------|-----|
| Full snapshot | `https://zavscom.github.io/adoni-directory-backend/data/adoni_full.json` |
| Delta | `https://zavscom.github.io/adoni-directory-backend/data/adoni_changes.json` |

No path changes are required: **`/data/*.json` in the repository is already the correct path** for that URL shape. Your Android app can `GET` these over HTTPS (consider caching, ETag, and handling offline).

**Note:** Pages serves the last successful build of the branch you selected; after the scraper workflow pushes new JSON, the site updates on the next Pages deployment (usually within a few minutes).
