# PixelCore advisor simulation

> Migration 2026-10-30: the TypeScript worker and Interactions mock described below were replaced by Spring AI in Java and the generateContent mock. Current commands and verification are in [docs/AI.md](../../docs/AI.md) and [pixelcore-advisor.context.yaml](pixelcore-advisor.context.yaml). The 2026-10-25 observations below are historical and do not establish the new implementation's behavior.

Recorded 2026-10-25. Reuses the repository's existing local mocks; no owning-team mock search was needed for this local contract subset.

## Purpose

Runs the real TypeScript advisor and Spring proxy against catalog and Gemini HTTP stand-ins. Handlers and modeled failures live in `../mocks/pixelcore-catalog` and `../mocks/pixelcore-gemini`; contracts live in `../specs`.

## Execution

Historical execution used simulation port 4900, worker 3002 and Spring 8081, leaving live services on 3001/8080. The old worker commands were retired on 2026-10-30. The simulation's `listening` event confirmed both member addresses. For request evidence, add `--output ndjson`, redirect to an ignored file under `tmp/ai-qa`, and group `request` events by `routeKey`. Stop the test processes after completion.

## Assumptions

- The second Spring process still needs a reachable database; only its AI dependencies are simulated. Flyway was disabled on that process to avoid concurrent migrations. (observed 2026-10-25)
- The simulation worker sets a 2-second timeout and elevated local rate limits. Restart it before repeating the suite, because the final quota test puts the worker into a 60-second cooldown. (observed 2026-10-25, `test/integration.mjs`)
- Passing this suite establishes HTTP wiring and failure handling, not Google credentials or model behavior. Separately, live `check:live` passed with Gemini 3.5 Flash-Lite and the real catalog on 2026-10-25, including a 25-to-30-million budget follow-up.

## Observations

<!-- begin:observations -->
2026-10-25: The revised suite passed through Spring 8081 with both `catalog` and `gemini` serving traffic. Zero-stock imported listings retained their exact inventory values and general advice omitted blanket warehouse warnings; explicit in-stock requests still returned no products. A separate live Gemini browser request returned a compact product card without the old warning.
2026-10-25: `test:integration` passed through Spring 8081. Both `catalog` and `gemini` served requests; neither member stayed silent. Responses covered recommendations, clarification, out-of-scope, invalid output, denied access, provider failure, timeout and quota. Requests rejected locally did not reach Gemini.
<!-- end:observations -->
