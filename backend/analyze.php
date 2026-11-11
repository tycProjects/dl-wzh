<?php
declare(strict_types=1);

require __DIR__ . '/config.php';

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

define('SYSTEM_PROMPT', <<<'PROMPT'
You are Logalyze, an expert Minecraft crash and log analyzer for Java Edition on PC and Android launchers (Zalith, PojavLauncher, Prism and others).
Read the log and diagnose the main problem.
Reply with ONE JSON object only, no markdown, with exactly these keys:
- errorType: short name, for example OutOfMemoryError, NoClassDefFoundError, Mod conflict, Missing dependency
- summary: one plain sentence
- confidence: integer from 0 to 100, how sure you are
- rootCause: 2 to 3 sentences in plain language
- evidence: array of 1 to 5 exact lines copied from the log
- fixes: array of 2 to 5 short actionable steps, most likely fix first
If the log shows no crash or error, set errorType to "No error found" and explain in summary.
Never invent log lines. Mention mod names or versions only if they appear in the log. Write in English.
PROMPT
);

function out(int $code, array $data): void
{
    http_response_code($code);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}

function fail(int $code, string $message): void
{
    out($code, ['ok' => false, 'error' => $message]);
}

function rate_limited(): bool
{
    $ip = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
    $file = sys_get_temp_dir() . '/logalyze_rl_' . md5($ip) . '.json';
    $now = time();
    $hits = [];
    if (is_file($file)) {
        $data = json_decode((string)@file_get_contents($file), true);
        if (is_array($data)) {
            foreach ($data as $t) {
                if (is_int($t) && $t > $now - 3600) {
                    $hits[] = $t;
                }
            }
        }
    }
    if (count($hits) >= RATE_LIMIT_PER_HOUR) {
        return true;
    }
    $hits[] = $now;
    @file_put_contents($file, json_encode($hits), LOCK_EX);
    return false;
}

function sanitize(string $t): string
{
    $rules = [
        '~[A-Za-z]:\\\\Users\\\\[^\\\\\s]+~i'                          => 'C:/Users/<user>',
        '~/(home|Users)/[^/\s]+~'                                      => '/$1/<user>',
        '~[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}~'            => '<email>',
        '~\b\d{1,3}(?:\.\d{1,3}){3}\b~'                                => '<ip>',
        '~--(accessToken|session|uuid|xuid|clientId)\s+\S+~i'          => '--$1 <redacted>',
        '~(accessToken|access_token|session|token|password|authorization)\s*[=:]\s*\S+~i' => '$1=<redacted>',
    ];
    foreach ($rules as $pattern => $replacement) {
        $res = preg_replace($pattern, $replacement, $t);
        if ($res !== null) {
            $t = $res;
        }
    }
    return $t;
}

function trim_log(string $t): string
{
    if (strlen($t) <= LOG_KEEP_HEAD + LOG_KEEP_TAIL) {
        return $t;
    }
    return substr($t, 0, LOG_KEEP_HEAD) . "\n...[middle of the log omitted]...\n" . substr($t, -LOG_KEEP_TAIL);
}

function parse_json_content(string $content): array
{
    $content = trim($content);
    $content = preg_replace('~^```(?:json)?\s*|\s*```$~i', '', $content) ?? $content;
    $start = strpos($content, '{');
    $end = strrpos($content, '}');
    if ($start === false || $end === false || $end <= $start) {
        throw new RuntimeException('AI did not return JSON');
    }
    $data = json_decode(substr($content, $start, $end - $start + 1), true);
    if (!is_array($data)) {
        throw new RuntimeException('AI returned invalid JSON');
    }
    return $data;
}

function clean_str($v, int $max): string
{
    return mb_substr(trim(is_string($v) ? $v : ''), 0, $max);
}

function clean_list($v, int $maxItems, int $maxLen): array
{
    $out = [];
    if (is_array($v)) {
        foreach ($v as $item) {
            $s = clean_str($item, $maxLen);
            if ($s !== '') {
                $out[] = $s;
            }
            if (count($out) >= $maxItems) {
                break;
            }
        }
    }
    return $out;
}

function normalize(array $r): array
{
    $c = $r['confidence'] ?? 0;
    if (is_numeric($c)) {
        $c = (float)$c;
        if ($c > 0 && $c <= 1) {
            $c *= 100;
        }
    } else {
        $c = 0;
    }
    $result = [
        'errorType'  => clean_str($r['errorType'] ?? '', 120),
        'summary'    => clean_str($r['summary'] ?? '', 300),
        'confidence' => max(0, min(100, (int)round($c))),
        'rootCause'  => clean_str($r['rootCause'] ?? '', 800),
        'evidence'   => clean_list($r['evidence'] ?? [], 5, 300),
        'fixes'      => clean_list($r['fixes'] ?? [], 5, 250),
    ];
    if ($result['errorType'] === '' || $result['summary'] === '') {
        throw new RuntimeException('Invalid AI response');
    }
    return $result;
}

function call_llm(string $log, string $launcher): array
{
    $payload = [
        'model'           => LLM_MODEL,
        'temperature'     => 0.2,
        'response_format' => ['type' => 'json_object'],
        'messages'        => [
            ['role' => 'system', 'content' => SYSTEM_PROMPT],
            ['role' => 'user', 'content' => "Launcher: {$launcher}\n\nLog:\n{$log}"],
        ],
    ];

    $ch = curl_init(rtrim(LLM_BASE_URL, '/') . '/chat/completions');
    curl_setopt_array($ch, [
        CURLOPT_POST           => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_TIMEOUT        => LLM_TIMEOUT,
        CURLOPT_HTTPHEADER     => [
            'Content-Type: application/json',
            'Authorization: Bearer ' . LLM_API_KEY,
        ],
        CURLOPT_POSTFIELDS     => json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_SUBSTITUTE),
    ]);
    $resp = curl_exec($ch);
    $code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_error($ch);
    curl_close($ch);

    if ($resp === false) {
        throw new RuntimeException('AI service unreachable: ' . $err);
    }
    if ($code < 200 || $code >= 300) {
        throw new RuntimeException('AI service error ' . $code . ': ' . substr((string)$resp, 0, 300));
    }
    $data = json_decode((string)$resp, true);
    $content = $data['choices'][0]['message']['content'] ?? '';
    return parse_json_content((string)$content);
}

// ---- request handling ----

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'GET') {
    out(200, ['ok' => true, 'service' => 'logalyze']);
}
if ($method !== 'POST') {
    fail(405, 'Method not allowed');
}

$token = $_SERVER['HTTP_X_APP_TOKEN'] ?? '';
if (!is_string($token) || !hash_equals(APP_TOKEN, $token)) {
    fail(401, 'Unauthorized');
}

if (rate_limited()) {
    fail(429, 'Too many requests. Try again later.');
}

$raw = (string)file_get_contents('php://input', false, null, 0, MAX_BODY_BYTES + 1);
if (strlen($raw) > MAX_BODY_BYTES) {
    fail(413, 'Log is too large');
}

$body = json_decode($raw, true);
$log = is_array($body) ? ($body['log'] ?? '') : '';
if (!is_string($log) || trim($log) === '') {
    fail(400, 'Missing "log" field');
}
$launcher = is_array($body) && is_string($body['launcher'] ?? null) ? $body['launcher'] : 'Unknown';
$launcher = substr(preg_replace('~[^A-Za-z0-9 ._-]~', '', $launcher) ?? 'Unknown', 0, 40);

try {
    $result = normalize(call_llm(trim_log(sanitize($log)), $launcher));
    out(200, ['ok' => true, 'result' => $result]);
} catch (Throwable $e) {
    error_log('[logalyze] ' . $e->getMessage());
    fail(502, 'Analysis failed. Try again.');
}
