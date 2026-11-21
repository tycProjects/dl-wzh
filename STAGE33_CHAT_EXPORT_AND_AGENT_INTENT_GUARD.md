# Stage 33 — Chat Export and Agent Intent Guard

## Fixes
- Chat export now writes a real JSON file directly to the public `Downloads/AI Island/` folder using MediaStore, then reports the saved filename/path. It no longer depends on a share-sheet target to make the export usable.
- The agent loop now rejects model-proposed device actions unless the user's own message matches a recognized device-command intent.
- The malformed-envelope recovery resolver is only invoked for recognized device commands; ordinary chat cannot be converted into an app-launch command by the fallback model.
- Intent classification explicitly rejects explanatory/question phrasing (e.g. `Coba jelasin dong`, `Jelaskan cara membuka WhatsApp`, `Bagaimana cara menyalakan Wi-Fi`).
- Added unit tests for ordinary conversation vs explicit device commands.

## Validation
- Source-level checks and ZIP integrity are run for this package.
- A full Gradle Release compile is not claimed unless the Gradle 8.9 distribution and Android dependencies are available in the build environment.
