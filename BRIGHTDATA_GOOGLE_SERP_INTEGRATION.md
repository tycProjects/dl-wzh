# Bright Data Google SERP integration — Stage 30

Android Island integrates the **Direct API Access** flow documented by the supplied
`luminati-io/Google-Search-API` repository.

## In-app setup

1. Open AI settings → Advanced settings.
2. Open **Google Search · Bright Data** and paste the Bright Data API token.
3. Open **Bright Data SERP zone** and enter the SERP zone name from the Bright Data dashboard.
4. The AI web-search action uses Bright Data Google SERP when both values are present.

The token and zone are stored through the app's Android Keystore-backed encrypted secret
storage. They are sent only to `https://api.brightdata.com/request`.

## Request flow

- `POST https://api.brightdata.com/request`
- `Authorization: Bearer <API_TOKEN>`
- JSON body: `zone`, Google search URL with `brd_json=1`, and `format: raw`.
- Parses `organic`, `news`, `videos`, `shopping`, and `local` result sections.
- Only HTTPS source links are passed into AI context.
- Search snippets are not treated as full-page verification. The AI is instructed to open
  original sources for important claims and not to invent publication dates.

## Provider precedence

- Current-news requests continue to use the existing publisher RSS pipeline.
- Other web searches: Bright Data SERP when token + zone are configured; otherwise Brave
  LLM Context when configured; otherwise the existing explicitly-limited DuckDuckGo Lite
  fallback.
- A failed Bright Data request is reported as a failed search, not silently represented as
  successful results.

## Deliberately not embedded

The supplied repository's Playwright/Chromium free scraper is not embedded in Android.
It requires a desktop browser runtime and its own automation dependencies. Native proxy
mode is also not embedded: it requires proxy credentials and transport behavior different
from the documented Direct API flow.

## Source attribution

Implementation follows the Direct API Access request format and SERP parameters documented
in the supplied repository: https://github.com/luminati-io/Google-Search-API
