# Update 1.16.0 — Multi-step agent observation and direct chat save

- Multi-step requests beginning with `open_app` no longer use the single-action shortcut. The shortcut is restricted to short, single-step open-app requests.
- Added unit coverage for multi-step TikTok/YouTube requests versus simple app-open commands.
- After actions, the agent now reads the foreground app through `IslandAccessibilityService.captureScreenContext()`, which hides the Island overlay and selects the external application window. It no longer uses the generic screen text reader for this agent-loop observation.
- After opening an app, the agent waits briefly and retries observation (without repeating the open action) to allow the destination app to finish launching.
- The chat export control is labeled `Simpan` with a download icon and saves JSON directly to `Downloads/AI Island` via MediaStore. It does not invoke ACTION_SEND or a share chooser.

Build/device validation is still required. This source update has not been verified on a physical Android device.
