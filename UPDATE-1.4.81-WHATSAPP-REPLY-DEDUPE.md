# Update 1.4.81 — WhatsApp reply and duplicate-notification fix

- Message deduplication now uses the newest MessagingStyle message rather than the entire conversation history.
- Dedupe fingerprints are tracked across notification keys, so reposts/recreated notification records do not replay the same message.
- Fallback fingerprints no longer include `postTime`, which changes on metadata-only reposts.
- A successful inline reply clears the complete mirrored WhatsApp banner stack. The listener's message fingerprint cache suppresses the same source message if WhatsApp reposts it, while a genuinely new message (new MessagingStyle timestamp/content) is still eligible.
- Existing source-notification removal behavior remains non-destructive; Android clearing/replacing its notification does not independently erase the Island-owned stack.

Validation performed: source-level inspection and ZIP integrity. A full Android Gradle/Kotlin build and device runtime test were not available in this environment.
