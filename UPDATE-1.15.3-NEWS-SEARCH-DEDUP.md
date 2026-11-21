# Update 1.15.3 — News/Search Deduplication

## Changes
- Prevents the AI from repeating an equivalent web search query during the same user turn; the existing observation is reused.
- Deduplicates DuckDuckGo results by normalized title and canonical URL (tracking/query parameters are ignored for URL identity).
- Deduplicates related-topic entries in Instant Answer responses.
- Limits deep-search page processing to 1–10 pages to keep the operation bounded.
- Updates the AI instruction to reuse search results instead of repeating the same query/story.

## Validation
- Kotlin smoke tests for normalized query identity, same-turn repeat detection, duplicate title removal, and canonical URL removal passed.
- Full Gradle unit tests and Android release build were not run in this environment because Gradle 8.9 could not be downloaded (DNS/network restriction).
- Live search-provider behavior and semantic duplicates with substantially different titles still require device/network testing.
