package com.example.crypto

enum class SecretFormat(val id: String, val displayNameEn: String, val displayNameBn: String, val icon: String) {
    STEGANOGRAPHY("stego", "Hidden In Emoji", "ইমোজির ভেতরে গোপন বার্তা", "🤔"),
    EMOJI("emoji", "Emoji Secret", "ইমোজি সিক্রেট", "🔐"),
    CHARACTER("character", "Character Cipher", "ক্যারেক্টার সাইফার", "⚡"),
    AES256_GCM("aes256", "AES-256-GCM Military", "মিলিটারি গ্রেড AES-256", "🛡️")
}
