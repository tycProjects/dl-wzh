<?php
// Edit these values on the server. Never put the API key inside the Android app.

define('LLM_BASE_URL', 'https://api.groq.com/openai/v1'); // any OpenAI-compatible endpoint
define('LLM_API_KEY',  'PUT_YOUR_API_KEY_HERE');
define('LLM_MODEL',    'llama-3.3-70b-versatile');          // check your provider's current model list
define('LLM_TIMEOUT',  45);

// Simple shared token the app sends in the X-App-Token header. Change it.
define('APP_TOKEN', 'change-this-token');

define('MAX_BODY_BYTES',      600000); // reject bigger requests
define('LOG_KEEP_HEAD',       3000);   // chars kept from the start of the log
define('LOG_KEEP_TAIL',       20000);  // chars kept from the end of the log
define('RATE_LIMIT_PER_HOUR', 20);     // requests per IP per hour
