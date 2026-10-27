# Azuro (Android)

Floating logo → ask about your screen. Two-model pipeline via OpenRouter:
1. **Vision model** reads the screenshot and returns a description + element boxes (0–1000 grid).
2. **Chat model** answers from that description and picks elements to mark.
Marks (box / arrow / circle / step) are drawn on the exact element boxes, so they land on target.

Build: open in Android Studio → Build > Build APK, or push to GitHub and run the "Build APK" workflow.
Use: open Azuro → add OpenRouter key → pick both models → Start. Tap the logo for chat, long-press it to clear marks, "Stop" in the notification to quit.
