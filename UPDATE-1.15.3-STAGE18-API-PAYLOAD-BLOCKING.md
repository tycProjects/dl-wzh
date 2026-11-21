# Stage 18 — API Payload Blocking, rebased on Stage 17

## Baseline and preserved stages
This source is rebased on `AndroidIsland_stage17_data_freshness.zip`, not the older Stage 15 archive. It therefore retains the Stage 17 cumulative project tree, including Stage 16 universal API data checks, Stage 15 sector/provider validation, Stage 14 strict response-structure validation, Stage 13 public API catalogue/runner audit, and the Stage 17 freshness/cache metadata work. Earlier project changes and update notes already present in the Stage 17 archive are retained.

## New changes in this stage
- Added `ApiPayloadGate` to block empty or metadata-only JSON, explicit provider errors, empty/null result containers, heavily null-valued payloads, definite impossible values detected by sector checks, missing required fields in the three narrow known-provider profiles, unstructured plain text, invalid structures, and truncated responses.
- Integrated the gate after the existing structural and sector checks in `PublicApis.get`. The response URL, HTTP status, retrieval time, provider/cache freshness metadata, and sector assessment remain visible, but a blocked response body is not passed to the AI.
- Updated `AiSession` to report a blocked payload distinctly.
- Updated `AiActionProtocol` to prohibit reconstructing blocked data from model memory and to preserve Stage 17 freshness/source caveats.
- Added focused unit tests for the gate.
- Bumped app version to 1.15.3 (versionCode 148).

## Important limitations
- Blocking is response-level, not a permanent host-wide blacklist. A provider host can serve multiple endpoints with different schemas; a bad response is blocked without assuming every endpoint on that host is bad.
- Passing the gate does not prove factual truth, source authority, completeness, or freshness. Unknown provider schemas receive only basic checks. XML remains well-formedness-only and has no general semantic schema validation.
- No live endpoint sweep was performed.

## Verification status
- Archive integrity and required-file checks are performed when packaging.
- Kotlin/Gradle unit tests and APK build require the Gradle distribution/dependencies. Earlier offline Gradle execution could not download Gradle 8.9 because network resolution was unavailable; no successful Android build or device test is claimed here.
