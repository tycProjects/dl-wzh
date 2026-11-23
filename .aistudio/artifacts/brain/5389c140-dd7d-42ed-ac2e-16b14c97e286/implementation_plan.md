# Touchline FM — World & Continental Expansion Plan

This plan expands Touchline FM into a global, multi-confederation football management simulation featuring authentic modern UEFA Swiss-format and AFC East/West continental tournaments, 18+ real-world national leagues with promotion/relegation and domestic cups, sharper high-contrast club crests across every screen, and deeper day-to-day management through an interactive Inbox, Youth Academy, Contract & Wage management, and a Detailed Match Center.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The decisions below reflect your confirmed choices from Phase 1 and establish the defaults we will implement across gameplay, UI navigation, and progression.

- **Navigation & Bottom Bar Preservation**: Keep the 4 core bottom tabs (**Club**, **Squad**, **League**, **Market**) untouched and introduce **contextual sub-tabs** inside each destination (`Club → Overview | Inbox`, `Squad → First Team | Youth Academy | Contracts`, `League → Pyramid | Domestic Cup | Continental`, `Market → Scouting | Transfer Offers`).
- **Continental Calendar Pacing**: Interleave UEFA (UCL, UEL, UECL) and AFC Champions League Elite matchdays across the domestic league season so league form, squad fatigue, injuries, and suspensions carry directly between domestic and continental fixtures.
- **Season 1 Continental Qualification**: Enforce **strict real-world qualification spots**. European and Asian Season 1 brackets are seeded from authentic top-flight standings/coefficients, while lower-tier or non-qualifying clubs must earn promotion and finish in real continental spots (`UCL`, `UEL`, `UECL`, or `ACL Elite`) to enter in future seasons.
- **Financial & Board Governance**: Use a **balanced cash-flow model**—weekly player wages deduct automatically from club cash each matchweek, board confidence rises/falls based on results and financial health, and contract renewals or urgent board warnings notify you in the Inbox before severe budget strain occurs.

---

## 1. Overview & Core Concept

- **What It Does**:
  - Transforms the Continental tab into four complete, selectable real-world tournaments: **UEFA Champions League** (36-team Swiss phase, 8 rounds), **UEFA Europa League** (36-team Swiss phase, 8 rounds), **UEFA Conference League** (36-team Swiss phase, 6 rounds), and **AFC Champions League Elite** (12–16 club **West Region** & 12–16 club **East Region** league stages leading into regional Round of 16 and centralized Quarter-Finals through Final).
  - Expands the global football pyramid with Portugal, Netherlands, Brazil, and Argentina (multi-tier pyramids with play-offs/relegation + domestic cups) alongside Turkey, Iraq, UAE, Qatar, Egypt, Japan, South Korea, USA (MLS), and additional UEFA European clubs—scaling clubs and players cleanly from elite 85–90 OVR down to 45–65 OVR with zero fictional teams or players.
  - Deepens club ownership with an actionable **Manager Inbox** (bid offers, injury/suspension reports, board reviews, continental draw notices), a **Youth Academy** (scouting and prospect promotion), **Contracts & Wages** (expiry tracking and renewal talks), and a **Detailed Match Center** (live ratings, cards, scorers/assists, and real-time mentality/pressing adjustments).
- **Target Audience**: Football management enthusiasts who want authentic modern tournament structures, multi-country progression, tactical control on matchday, and crisp mobile readability in dark mode.
- **Key Value**: Every fixture, trophy, youth prospect, wage bill, and tactical tweak connects in one fast, offline-ready career loop without cluttering the 4-tab mobile shell.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    TOUCHLINE FM 4-TAB SHELL (DARK MODE)                 │
├───────────────┬───────────────────┬──────────────────┬──────────────────┤
│  CLUB TAB     │   SQUAD TAB       │   LEAGUE TAB     │   MARKET TAB     │
├───────────────┼───────────────────┼──────────────────┼──────────────────┤
│ • Overview    │ • First Team      │ • League Pyramid │ • Transfer Pool  │
│   Next Match, │   Pitch, Subs,    │   18+ Nations,   │   Filters, Wage  │
│   Training,   │   Lineup Status   │   Tables & MDs   │   Fit & Bids     │
│   Board Meter │   (Inj/Susp)      │ • Domestic Cup   │ • Club Valuation │
│ • Inbox [N]   │ • Youth Academy   │   Bracket + POs  │   & Outgoing     │
│   Bids, Draws,│   Scout Missions, │ • Continental    │   Negotiations   │
│   Alerts      │   Promote U19s    │   UCL/UEL/UECL   │                  │
│               │ • Contracts       │   & ACL Elite    │                  │
│               │   Wages, Renewals │   (West | East)  │                  │
└───────────────┴───────────────────┴──────────────────┴──────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    DETAILED LIVE MATCH CENTER                           │
│  Broadcast Scoreboard (Large Crests, Scorers, Clock, Pens if KO)        │
│  ├── Live Tactics Strip: Mentality (Defensive..All-Out) + Press Level   │
│  ├── Timeline & Key Events (Goals/Assists, Cards, Injuries, Subs)       │
│  ├── Live Player Ratings (1.0–10.0) & Team Stats Comparison Bars        │
│  └── Live 5-Sub Workbench & Post-Match Press Conference Trigger         │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Continental Tournament Switcher (`League → Continental`)**:
  - Top horizontal scrollable selector pills let the user inspect **UCL (36)**, **UEL (36)**, **UECL (36)**, and **ACL Elite (West / East)** independently of their own club's qualification.
  - Each UEFA tournament displays: unified **1–36 Swiss Table** with color-coded qualification bands (**1–8 R16 Direct**, **9–24 Two-Legged Play-off**, **25–36 Eliminated**), **Swiss Matchday Browser** (`MD 1–8` or `1–6`), **Two-Legged Play-off Aggregate Ties**, and the **R16 → QF → SF → Final** knockout bracket.
  - **ACL Elite** provides a clean **West Region | East Region** switcher showing separate regional league tables (top 8 advancing per region), **Regional R16**, and the **Centralized Cross-Region Final Stages (QF → SF → Final)** pitting West survivors against East survivors.
- **Manager Inbox (`Club → Inbox`)**:
  - Displays an unread badge count on the `Inbox` sub-tab pill.
  - Filterable feed (`All`, `Offers`, `Medical & Discipline`, `Board & Draws`). Incoming transfer bids from AI clubs include **Accept Fee** or **Reject** buttons that execute immediately against budget and squad size.
- **Youth Academy & Contracts (`Squad → Youth | Contracts`)**:
  - **Youth Academy**: Shows 4–6 academy prospects (ages 15–18) with star potential ratings, nationality matching club/scouted region, and a **Scout New Intake** action plus **Promote to Senior XI** / **Release**.
  - **Contracts**: Displays total weekly wage bill vs. weekly club revenue, expiring contracts (`1 yr left` highlighted in amber), and a **Renew Contract** modal sheet with offered wage & term sliders.
- **Detailed Match Center**:
  - Replaces generic commentary-only view with a **Tactics Bar** (Mentality + Pressing Intensity directly adjusting match engine attack/defense/stamina rates), **Key Events Filter / Timeline** with goal+assist badges, yellow/red cards, and a **Ratings & Lineups** tab showing live 1.0–10.0 match ratings for every player on the pitch.

### Visual Identity & Mobile Polish

- **Aesthetic Direction**: High-contrast tactical broadcast room — retains the existing **PitchBlack** canvas, **ObsidianSurface** headers, **SlateCard** containers with crisp 1dp hairline borders, and **TurfEmerald / ElectricCyan / BroadcastAmber / CrimsonRed** status accents.
- **Typography & Anti-Clipping Rules**:
  - Keep **Space Grotesk** (headers/scorelines), **Plus Jakarta Sans** (body/labels), and **JetBrains Mono** (standings, OVRs, wages, match clock).
  - Eliminate any clipped pills or truncated tab titles on narrow phones by using **scrollable sub-tab rows (`SecondaryScrollableTabRow` / horizontal `LazyRow` pills with `softWrap = false`)**, compact badge copy on tabs, and adaptive horizontal padding (`minHeight = 40.dp` chips, full label visibility).
- **Upgraded High-Contrast Club Crests**:
  - Scale crest sizes up meaningfully across all screens: **36dp–40dp** in league/continental tables and fixture cards (up from 26–30dp), **58dp–68dp** in Club Selection, Matchday Hero, and Scoreboard headers.
  - Enhance procedural vector crest rendering (`ClubBadge`) with richer inner metallic rims, subtle radial depth shading, bolder stripes/hoops/sashes, larger legible heraldic symbols, and dark pill backing behind monograms—while rendering every custom SVG crest (such as the 16 Saudi Football badges) cleanly at every resolution.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Strict Separation of UEFA (108 Clubs) & AFC (24–28 Clubs) Pools**:
  - *Chosen Approach*: Seed 36 real clubs in UCL, 36 in UEL, and 36 in UECL from our expanded European leagues (England, Spain, Italy, Germany, France, Portugal, Netherlands, Turkey) plus authentic European league champions/qualifiers from Scotland, Belgium, Austria, Greece, Ukraine, Croatia, Serbia, Czechia, Switzerland, and Denmark. Seed ACL Elite West (Saudi, UAE, Qatar, Iraq, Iran, Uzbekistan) and East (Japan, South Korea, China, Australia, Thailand) with real regional clubs.
  - *Why*: Ensures true 36-team Swiss tables and authentic West/East Asian brackets with zero club duplication across tournaments, while keeping player/club counts modular and fast.
  - *Alternatives Considered*: Mixing the same clubs across UCL/UEL or shrinking tables to 16 teams—rejected because the prompt explicitly requires authentic 36-team Swiss formats and full East/West splits.

- **Decision 2 — Hybrid Match Resolution Speed**:
  - *Chosen Approach*: Simulate the user's live match and user's domestic division / active continental group rivals with full minute-by-minute events and player stats; resolve off-screen world league and distant AI cup fixtures via a calibrated Poisson strength engine using OVR, home advantage, and tactical modifiers.
  - *Why*: Keeps advancing matchdays instantaneous (<100ms) even with 18+ countries and 3×36-team Swiss competitions running concurrently.
  - *Alternatives Considered*: Full 90-minute minute-by-minute event generation for 250+ simultaneous AI matches—rejected due to battery/CPU lag on mobile devices.

- **Decision 3 — Interleaved Season Calendar Engine**:
  - *Chosen Approach*: Drive progression via a unified season calendar milestones tracker (`League MD`, interleaved `Continental Swiss MD` / `Knockout Leg`, `Domestic Cup Round`, `Play-offs`) so the main **Kick Off** / **Advance Stage** hero button always surfaces the exact next match in chronological order.
  - *Why*: Preserves the effortless single-button career cadence while fitting 6–8 Swiss continental matchdays, two-legged play-offs, and domestic cups cleanly into one season.

- **Decision 4 — Graceful Save Migration**:
  - *Chosen Approach*: Bump/migrate `CareerSaveState` defaults cleanly so newly added fields (`inboxMessages`, `youthAcademy`, `boardConfidence`, `continentalTournaments`) initialize safely, while unstarted or legacy saves upgrade seamlessly to the expanded world database.

---

## 4. Technical Architecture & Data Strategy

### System Architecture & Data Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          WORLD DATA MODULES                             │
│  EnglandData (T1-12) │ SaudiData (T1-3) │ EuroPyramids (ESP/ITA/GER/FRA)│
│  EuroNewData (POR/NED/TUR + UCL/UEL/UECL Clubs)                         │
│  AmericasData (BRA T1-2, ARG T1-2, MLS)                                 │
│  AsiaAfricaData (IRQ, UAE, QAT, EGY, JPN, KOR, IRN/UZB/CHN/AUS/THA)     │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ WorldDatabase + ClubSvgBadges
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     COMPETITION & SIMULATION ENGINES                    │
│  • ContinentalEngine: Swiss-36 Pairings (4 Pots, 8/6 Rounds),           │
│    2-Legged Play-offs (9-24), R16->Final; ACL Elite West/East + Final 8 │
│  • CareerScheduler: Interleaved League + Cup + Continental Calendar     │
│  • MatchEngine: Live Mentality/Pressing Modifiers, Assists, Cards,      │
│    Injuries, Live Player Ratings, Penalties & Two-Leg Aggregate Logic   │
│  • CareerEventsEngine: Weekly Wages, Contract Expiry, Youth Scouting,   │
│    AI Transfer Offers, Suspensions, Board Confidence & Inbox Alerts     │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ StateFlow<CareerSaveState>
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    CAREER VIEWMODEL & REPOSITORY                        │
│  Moshi JSON Persistence + Match Ticker + Interactive Actions            │
│  (Inbox Offer Response, Youth Promote, Contract Renew, Live Tactics)    │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ Compose State Collection
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                 4-TAB JETPACK COMPOSE UI + MATCH CENTER                 │
│  Club (Overview | Inbox)  • Squad (First Team | Youth | Contracts)      │
│  League (Pyramid | Domestic Cup | Continental) • Market (Pool | Deals)  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Core Data Models & Modular Strategy

- **Expanded Country & Division Registry**:
  - Expand `CountryCode` to cover **ENG, KSA, ESP, ITA, GER, FRA, POR, NED, BRA, ARG, TUR, IRQ, UAE, QAT, EGY, JPN, KOR, USA**, plus an international UEFA/AFC pool tag (`EUR_OTHER`, `AFC_OTHER`) so clubs from Scotland, Belgium, Austria, Iran, Uzbekistan, China, Australia, Thailand, etc. compete in continental tournaments without cluttering domestic pyramid pickers.
  - Keep England (Tiers 1–12), Saudi Arabia (Tiers 1–3), Spain (T1–T3), Italy (Serie A, Serie B, Serie C + Coppa Italia), Germany (Bundesliga, 2. Bundesliga, 3. Liga + DFB-Pokal), France (T1–T3), and add Portugal (2 tiers + Taça de Portugal), Netherlands (2 tiers + KNVB Cup), Brazil (Série A & B + Copa do Brasil), Argentina (Liga Profesional & Primera Nacional + Copa Argentina), and full authentic Tier 1 leagues + domestic cups for Turkey, Iraq, UAE, Qatar, Egypt, Japan, South Korea, and USA (MLS).
- **Continental Tournament State (`ContinentalTournamentState`)**:
  - Encapsulates four distinct tournaments (`UCL`, `UEL`, `UECL`, `ACL_ELITE`) rather than a single flat list:
    - Each UEFA tournament stores: `participantIds` (36 clubs), `leaguePhaseStandings` (1–36 sorted by Points, GD, GF), `leaguePhaseFixtures` (8 rounds for UCL/UEL, 6 for UECL using balanced 4-pot pairings), `playoffFixtures` (two-legged ties for seeds 9–24), `knockoutFixtures` (R16, QF, SF, Final), and `championClubId`.
    - `ACL_ELITE` stores separate `westStandings` & `westFixtures` (12–16 West clubs), `eastStandings` & `eastFixtures` (12–16 East clubs), `westR16` / `eastR16`, and unified `finalStages` (centralized QF, SF, Final pairing West vs. East).
- **Career Depth Models**:
  - **Contracts & Finances**: Extend `Player` with `contractYearsLeft: Int`, `potential: Int`, `suspendedMatches: Int`, `injuredMatches: Int`, and add `weeklyRevenueM: Double` + `boardConfidence: Int` (`0..100`) to `Club` / `CareerSaveState`.
  - **Youth Prospects (`YouthProspect`)**: Stores `id`, `name`, `age` (15–18), `nationality`, `position`, `overall` (52–68 scaled to club tier), `potential` (68–92), `wageK`, and scouting week discovered.
  - **Manager Inbox (`InboxMessage`)**: Stores `id`, `category` (`TRANSFER_OFFER`, `BOARD`, `MEDICAL`, `CONTINENTAL`, `DISCIPLINE`), `title`, `body`, `matchdayLabel`, `isRead`, plus optional `OfferPayload` (`playerId`, `buyingClubId`, `buyingClubName`, `offerAmountM`, `resolvedStatus`).
- **Live Match Center Extensions (`LiveMatchSession` & `MatchTactics`)**:
  - Track `userMentality` (`DEFENSIVE`, `BALANCED`, `ATTACKING`, `ALL_OUT_ATTACK`) and `userPressing` (`LOW_BLOCK`, `MID_PRESS`, `HIGH_PRESS`) dynamically adjustable mid-match.
  - Maintain live `playerRatings: Map<String, Double>` updated by goals (+1.1), assists (+0.6), saves (+0.3), cards (-0.5 / -1.5), and minutes played, displayed alongside scorers/assists and card badges inside the Match Center.
- **Interactive State Mapping**:
  - Tapping **Accept Offer** on an Inbox transfer bid credits club cash, removes the player from the squad (auto-rebalancing XI/bench if needed), logs a headline, and marks the offer resolved.
  - Tapping **Promote** on a Youth prospect moves them into the senior squad on a 3-year youth wage contract.
  - Adjusting **Mentality / Pressing** during a live match immediately updates `LiveMatchSession` multipliers for subsequent simulated minutes.
  - Switching tournaments (`UCL`, `UEL`, `UECL`, `ACL_ELITE`) or `West | East` region rebinds the visible table, qualification bands, Swiss rounds, and knockout bracket cleanly without mutating career progress.
