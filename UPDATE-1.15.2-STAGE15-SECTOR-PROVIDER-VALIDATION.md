# Stage 15 — Sector and selected-provider response validation

## Goal
Add conservative semantic sanity checks after strict JSON/XML structure validation. The checks must not claim factual correctness, completeness, or freshness.

## Changes
- Added `ApiDomainValidator.kt` and wired it into `PublicApis.get()`.
- Resolve the catalog category/provider from the final allowed response host; include the detected provider and sector in the observation passed to the AI.
- Add narrow provider profiles for:
  - Open-Meteo: common coordinate, timezone, data-block, and units fields.
  - Frankfurter: base currency, date, and rates object; three-letter rate keys and positive numeric values.
  - REST Countries: country name and common country identity/population fields.
- Add conservative category-level sanity checks for weather, currency exchange, geocoding/location, finance/crypto, health, country/government, sports/fitness, and transport.
- Health payloads always carry a warning that automated checks do not assess diagnosis, safety, or clinical recommendations.
- Unknown categories/providers are explicitly reported as lacking a sector/provider-specific profile rather than being presented as validated.
- AI instructions now state that absence of warnings does not establish truth.
- Added unit tests for provider resolution, provider profiles, sector checks, missing units/timestamps, suspicious numeric values, and unsupported categories.

## Important limitations
- This is not a full schema registry and does not validate every one of the 1,064 catalog entries.
- Provider profiles are intentionally limited to Open-Meteo, Frankfurter, and REST Countries. They are partial response-shape checks, not complete official OpenAPI/JSON Schema validation.
- Category checks are heuristic and may produce warnings for APIs with legitimate but different response shapes. A warning is a request to inspect documentation, not proof of bad data.
- No endpoint was contacted as part of this source-code change. Live availability, authentication requirements, freshness, factual accuracy, and provider terms remain unverified.
- The runner remains GET-only, HTTPS-only, and does not send API keys/OAuth/custom authentication headers.
- No Gradle test, APK build, or device test can be claimed unless executed successfully in an environment with the required Gradle distribution and dependencies.
