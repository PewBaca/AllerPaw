# AllerPaw

**AllerPaw** is an Android app for managing the nutrition and health of dogs — with a focus on allergy tracking, BARF feeding, and symptom documentation.

It connects to your Google Spreadsheets as a database and runs entirely without a backend.

## What it does

- Track allergy symptoms, food reactions, and environmental factors for your dog
- Manage ingredients and calculate nutritional values across 39 NRC nutrients
- Build and analyze BARF recipes with a food calculator
- Log pollen levels, weather data, and indoor climate (relevant for mite allergies)
- View statistics, correlations, and symptom patterns over time
- Export vet reports as PDF

## Status

> Version 0.1 — Android migration in progress  
> Previously a web app (GitHub Pages + Vanilla JS); now being rebuilt as a native Android app.

## Tech Stack (Target)

| Area | Technology |
|------|-----------|
| Platform | Android (API 26+) |
| Language | Kotlin |
| UI | Jetpack Compose |
| Database backend | Google Sheets API v4 (REST) |
| Auth | Google OAuth2 |
| Charts | MPAndroidChart or Vico |
| Offline storage | Room (local cache) |

## Note

This project started as "vibe coding" with AI assistance — not developed by a professional programmer. The food calculator's approach has been reviewed and approved by a nutritionist (for generally healthy dogs). The app will not warn you about every possible mistake.

Currently German-language only.

## GitHub

Hosted on GitHub. Releases are published as APKs.
