# Android Island AI Agent — Stage 27 Recovery Design

## Shared architecture

All model providers are interchangeable inference adapters. Planning, action validation,
execution, observation, retry policy, file creation, and completion verification belong to
one provider-independent runtime. A provider switch must never change safety rules.

## Observe → plan → validate → act → verify

1. Capture only the context needed for the current step (screen, app, status, or API result).
2. Ask the selected model for exactly one next action in the strict action schema.
3. Parse and allow-list the action. Unknown actions are discarded.
4. Apply risk/permission gates before execution. Destructive operations are refused; sensitive
   operations require explicit confirmation.
5. Execute through the narrowest capable interface: Android intent/API, Accessibility, then
   Shizuku only for operations Shizuku can actually perform and only when its service is ready.
6. Read the resulting state and give the model the actual observation, not an assumed success.
7. Continue until the goal is verified, a user confirmation is needed, or bounded recovery is
   exhausted.

## Recovery rules added in Stage 27

- A failed action is remembered for the current request. The exact same failed action is not
  replayed; the model must choose a different route.
- The recovery prompt asks for a fresh observation first, then a different UI/Android action,
  and Shizuku only when the Shizuku service is confirmed ready.
- The model may not bypass Android permission dialogs, safety checks, or user confirmation.
- If no safe alternative exists, the agent reports the real blocker instead of fabricating success.
- Existing global limits (24 action steps, 3 minutes, one action per observation cycle) remain in
  place. API retries and provider/key rotation remain separate from device-action retries.

## Shizuku's role

Shizuku is an optional execution adapter, not a magic permission bypass. It can provide shell
operations that Android exposes through the Shizuku service after the user grants access. The app
must check service readiness at runtime, validate shell commands with ShellPolicy, require
confirmation for risky operations, and refuse destructive operations. If Shizuku is unavailable,
the agent should use a supported Android intent, Accessibility path, or explain the manual step.

## Completion and reliability invariants

- A model's `done=true` is not proof. The runtime must inspect the result/observation before
  presenting completion.
- A timeout or empty response is unknown, not success.
- A provider error may trigger configured key/provider fallback; it must not silently change the
  user's selected model during an ordinary successful request.
- No retry loop is unbounded. Repeated identical failed actions are blocked to avoid wasted time,
  token use, and duplicate side effects.
- Downloadable output must be materialized as an actual local file and attached through the app's
  Android file-sharing/download flow; a filename or markdown link alone is not a file.

## Not claimed as complete in this stage

This stage adds bounded action recovery and tests. It does not claim full end-to-end device testing,
full provider conformance testing, or completion of Gemini Interactions API stateful storage and
server-side deletion. Those require their own integration work and live credentials/device tests.
