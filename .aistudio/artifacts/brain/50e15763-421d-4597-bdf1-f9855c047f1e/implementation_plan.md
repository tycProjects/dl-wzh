# ZMAPS — Native Android Navigation App by JUSSAR GOLDEN

A native Android navigation app built with Jetpack Compose featuring real Google Maps rendering, location tracking, destination search with bookmarks, driving route preview with distance/ETA step summaries, and customizable neon accent themes.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following key decisions were confirmed during the planning clarification:
> - **API Key Handling**: Google Maps & Routes API keys configured via the AI Studio Secrets panel (`.env` with `MAPS_API_KEY`). A dedicated status/setup guide banner will appear if the key is not yet configured, keeping the entire interface stable and explaining the required APIs.
> - **Route Preview Flow**: Focused on Driving routes providing real polyline geometry, trip distance, ETA duration, and maneuver step overviews retrieved from Google Maps Platform APIs.
> - **Search & Saved Features**: Search tab will feature real-time destination lookup, recent search history, and one-tap bookmarking for favorite locations stored locally.

---

### 1. Overview & Core Concept

- **What It Does**: ZMAPS delivers a sleek, high-contrast navigation experience for drivers and urban explorers. It features a full-screen interactive Google Map, current location centering, place search, destination cards, route calculations with step previews, and custom cyber-aesthetic theming.
- **Target Audience / Persona**: Drivers, commuters, and tech enthusiasts seeking a responsive, hacker-inspired map experience with zero clutter, high readability at night, and rapid access to saved destinations.
- **Key Value**: Delivers genuine Google Maps capabilities wrapped in a distinct, dark-mode visual identity with immediate accent switching, robust permission handling, and zero fake simulation states.

---

### 2. User Experience & Visual Design

#### Visual Identity & Theme
- **Background Atmosphere**: Deep near-black (`#050705`) for maximum contrast and battery efficiency on OLED screens.
- **Card & Surface Tiers**: Layered dark charcoal (`#101510` and `#182018`) with subtle border glows.
- **Primary Accent**: Neon Green (`#39FF14`) as default, with instant user-switchable themes:
  - **Neon Green**: `#39FF14`
  - **Electric Blue**: `#00D4FF`
  - **Vivid Red**: `#FF3344`
  - **Crisp White**: `#F5F5F5`
  - **Cyan**: `#00FFE0`
  - **Cyber Violet**: `#C74BFF`
- **Typography & Iconography**: Clean sans-serif with bold high-contrast headlines, crisp mono-style telemetry readouts for coordinates/ETA/distance, and Material 3 icons accented with the active neon glow.
- **Brand Mark**: Custom ZMAPS logo combining a sharp stylized geometric 'Z' integrated with a location pin concept, paired with "By JUSSAR GOLDEN" subtitle.

#### Key User Flows
1. **App Launch & Welcome Splash**:
   - Displays the stylized ZMAPS emblem and "By JUSSAR GOLDEN" attribution before revealing the main navigation canvas.
   - Restores saved accent color and language preference from local DataStore.
2. **Interactive Map Exploration**:
   - Google Map fills the screen with dark-mode map styling.
   - Top floating Search bar with quick clear and search trigger.
   - Floating Action Button for Location Centering (invoking runtime permission flow with explanatory rationales).
   - Bottom Navigation Bar with 3 tabs: **Map**, **Search/Saved**, and **Settings**.
3. **Place Search & Selection**:
   - Dedicated Search & Saved screen with quick search input, search history chips, and bookmarked destinations (Home, Work, Custom Pins).
   - Selecting a location centers the map, drops an accent-colored pin, and brings up the Destination Card.
4. **Route Preview & Step Summary**:
   - Destination Card displays place name, address, and a "Route Preview" button.
   - When tapped, fetches the driving route via Google Maps Platform Routes API.
   - Renders polyline overlay on the map, calculates bounding camera view, and opens the Route Details Sheet showing trip distance (km/mi), ETA, and step-by-step maneuver instructions.
5. **Settings & Customization**:
   - Color accent selector (6 colors) with instant reactive update across all screens.
   - Language selector supporting English, Filipino (Tagalog), Cebuano (Bisaya), and Indonesian (Bahasa Indonesia) with instant string re-binding.
   - Setup status card indicating Google Maps API key readiness.
   - About ZMAPS section featuring app version, developer credits (JUSSAR GOLDEN), and safety notices.

---

### 3. Key Product Decisions & Trade-Offs

- **Google Maps SDK for Compose (`maps-compose`)**:
  - *Chosen Approach*: Official `com.google.maps.android:maps-compose` library with `GoogleMap`, `Marker`, and `Polyline` composables.
  - *Why*: Provides reactive state integration with Jetpack Compose, native pinch-to-zoom, tilt, rotate gestures, and hardware-accelerated vector map rendering.
  - *Fallback / Unconfigured Key Handling*: If the API key is missing or invalid, shows a prominent cybernetic setup card guiding the user to add `MAPS_API_KEY` in the Secrets panel, rather than crashing or showing a blank broken screen.
- **Routing Engine**:
  - *Chosen Approach*: Direct integration with Google Maps Directions/Routes REST API via OkHttp/Retrofit, parsing polyline points, distance text, duration text, and maneuver steps.
  - *Why*: Directly leverages the user's Google Maps API key without requiring heavy external navigation SDKs, keeping APK size lean and builds fast.
- **Local Persistence via DataStore**:
  - *Chosen Approach*: Jetpack DataStore Preferences for accent color choice, language choice, recent searches, and saved favorite bookmarks.
  - *Why*: Asynchronous, thread-safe, non-blocking persistence compliant with Google Play guidelines.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                              │
│         Theme Wrapper (Dynamic Accent Scheme & Localized Context)       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Navigation & Scaffolding                        │
│             Top Bar / Floating Search | Bottom Navigation Bar           │
└───────┬───────────────────────────┼────────────────────────────┬───────┘
        │                           │                            │
        ▼                           ▼                            ▼
┌───────────────────┐     ┌───────────────────┐     ┌────────────────────┐
│     MapScreen     │     │ SearchSavedScreen │     │   SettingsScreen   │
│ - GoogleMap View  │     │ - Search Bar      │     │ - Accent Color (6) │
│ - Location Button │     │ - Recent Queries  │     │ - Language (4)     │
│ - Destination Card│     │ - Saved Bookmarks │     │ - API Status & Info│
│ - Route Polyline  │     │ - Category Filters│     │ - About ZMAPS      │
└─────────┬─────────┘     └─────────┬─────────┘     └──────────┬─────────┘
          │                         │                          │
          └─────────────────────────┼──────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        ZMapsViewModel & State                          │
│   MapState, SelectedLocation, RouteState, UserPreferences, SearchHistory│
└───────────────────┬───────────────────────────────┬────────────────────┘
                    │                               │
                    ▼                               ▼
┌──────────────────────────────────────┐  ┌──────────────────────────────┐
│          Repository & APIs           │  │     DataStore Storage        │
│ - LocationProvider (FusedLocation)   │  │ - Selected Accent Color      │
│ - DirectionsService (Routes/Retrofit)│  │ - Selected Language          │
│ - PlacesSearchHelper                 │  │ - Saved Bookmarks & History  │
└──────────────────────────────────────┘  └──────────────────────────────┘
```

#### Data Model Entities
- `ZPlace`: `id`, `name`, `address`, `latitude`, `longitude`, `isBookmarked`
- `ZRoute`: `points` (List of LatLng), `distanceText`, `durationText`, `startAddress`, `endAddress`, `steps` (List of `ZRouteStep`)
- `ZRouteStep`: `instruction`, `distanceText`, `durationText`, `maneuver`
- `ZMapsPreferences`: `accentColor` (GREEN, BLUE, RED, WHITE, CYAN, VIOLET), `language` (EN, FIL, CEB, ID)

#### Component & State Flow
- `LocationClient`: Uses `LocationServices.getFusedLocationProviderClient`, checks runtime permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`), provides current coordinates with graceful fallback if disabled.
- `DirectionsRepository`: Encodes start/end LatLng, requests route from Google Maps Platform, parses overview polyline into `List<LatLng>`, formats duration & distance.
- `PreferencesRepository`: Flow-based DataStore manager for persistent settings and saved places.
