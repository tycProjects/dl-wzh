# Update 1.9.9 — Jelly Motion Restored

- Restored the elastic overshoot for the **Opsi 3 · Jelly** morph in both opening and closing directions.
- `Elasticity` now controls the spring damping: NONE is restrained, while LOW/MEDIUM/HIGH progressively add more stretch and rebound.
- The SMOOTH morph path also gets a restrained Jelly crest when Opsi 3 is selected.
- The existing GPU squash-and-stretch layer and reserved layout slack are retained so the stretched panel is less likely to be clipped.
- Opsi 1 and Opsi 2 retain the stable, non-bouncy morph behavior.

Build note: compilation still needs to be verified in an environment with the configured Gradle distribution available.
