package com.zavscom.adonidirectory.sync

/**
 * Full snapshot JSON on the default branch (`main`).
 *
 * Prefer **raw.githubusercontent.com** over GitHub Pages: Pages can lag behind pushes or sit behind
 * a CDN that still serves an older file briefly. Raw URLs reflect the latest commit on [branch].
 *
 * If your fork/repo name differs, change `OWNER/REPO` below. After changing, pull-to-refresh in the app
 * (toolbar refresh) or wait for the next background sync.
 */
const val FULL_URL =
    "https://raw.githubusercontent.com/zavscom/adoni-directory-backend/main/data/adoni_full.json"
