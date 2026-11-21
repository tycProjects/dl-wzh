# Android Island 1.15.4 — Stage 12: News source and shell prefix correction

## Root-cause findings

- The prior Stage 11 change explicitly allowed `/shelldumpsys ...`. That was the opposite of the intended UX. `/shell` must be a separate prefix followed by whitespace. Compact forms are now rejected.
- Current-news requests were not using the bundled Public APIs catalog. They were routed to the generic DuckDuckGo `WebSearch.deepSearch` path. The catalog lookup (`api_find`) only finds candidate APIs; it does not itself execute a news provider.
- The bundled News entries include Noozra, Inshorts News, and Spaceflight News. Noozra's official docs state that the JSON API requires a bearer API key; the official `llms.txt` documents keyless per-category RSS feeds at `https://noozra.com/rss/<slug>.xml`. Inshorts is explicitly an unofficial India-focused source in its repository docs, and Spaceflight News is space-news-only.

## Changes

- Reverted compact shell prefix acceptance. `/shell dumpsys ...` and standalone `/shell` remain valid; `/shelldumpsys ...` is not recognized as a terminal command.
- Updated the command help and model instructions so they teach the required space and never recommend compact syntax.
- Added `NoozraNews`, which retrieves documented public category RSS feeds (not the key-protected JSON API), parses publisher headline/source/original URL/publication date/description, deduplicates items, and only returns items with parseable dates within the last seven days.
- Current-news requests in `WebSearch.deepSearch` now use this RSS retrieval. If no dated items can be retrieved, the model receives an explicit failure and must not fill gaps from memory.
- Updated the local Noozra catalog description to distinguish public RSS from the JSON API's key requirement.
- Added unit tests for strict shell syntax and RSS parsing/category selection.

## Limitations

- Noozra is a news aggregator; its own terms say the original publisher is the source of record. The assistant must show each publisher and original article URL and must not treat Noozra as the journalist/publisher.
- RSS feed reach and categories are limited; this is not exhaustive coverage and does not guarantee Indonesian domestic coverage.
- The JSON API is not called: it requires a bearer key, and the app currently has no dedicated Noozra credential setting. No API key is hard-coded or silently shared.
- Gradle compilation, live RSS execution from the Android app, and device tests must be reported separately; static checks are not a substitute.
