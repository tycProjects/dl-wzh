package com.tyce.enhancer

/**
 * Central place for outbound links. Replace the empty strings with your real URLs;
 * a row with an empty URL is hidden from the UI instead of pointing to a made-up account.
 */
object AppConfig {
    const val APP_NAME = "Alter Enhancer"
    /** Telegram channel link, e.g. "https://t.me/yourchannel". */
    const val TELEGRAM_URL = "https://t.me/tycept"
    /** Instagram profile link, e.g. "https://instagram.com/yourname". Empty = hidden. */
    const val INSTAGRAM_URL = ""
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val TELEGRAM_PACKAGE = "org.telegram.messenger"
}
