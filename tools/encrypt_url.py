#!/usr/bin/env python3
"""
ZewMod — URL Encryptor
Tool untuk generate XOR-encrypted URL bytes
yang dipakai di zewmod_native.c

Usage:
    python3 encrypt_url.py <url> <key>

Example:
    python3 encrypt_url.py "https://yourserver.com/patch" "Bz\$W8"
    python3 encrypt_url.py "https://yourserver.com/config" "c(?CK4X"

Output: C array literal untuk di-paste ke zewmod_native.c

Reverse dari obfuscation pattern di libnikumods_native.so:
  Keys ditemukan: "Bz$W8", "c(?CK4X", "4% X~;", "xv(,2K&", "iq80"
"""

import sys


def xor_encrypt(data: str, key: str) -> bytes:
    data_bytes = data.encode("utf-8")
    key_bytes = key.encode("utf-8")
    return bytes([b ^ key_bytes[i % len(key_bytes)] for i, b in enumerate(data_bytes)])


def to_c_array(name: str, data: bytes) -> str:
    hex_vals = ", ".join(hex(b) for b in data)
    lines = [
        f"static const unsigned char {name}[] = {{",
        f"    {hex_vals}",
        f"}};",
        f"/* Decrypts to: {len(data)} bytes */"
    ]
    return "\n".join(lines)


def verify_decrypt(data: bytes, key: str) -> str:
    key_bytes = key.encode("utf-8")
    return bytes([b ^ key_bytes[i % len(key_bytes)] for i, b in enumerate(data)]).decode("utf-8")


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)

    url = sys.argv[1]
    key = sys.argv[2]

    encrypted = xor_encrypt(url, key)
    print(f"\n# URL   : {url}")
    print(f"# Key   : {key}")
    print(f"# Bytes : {len(encrypted)}\n")
    print(to_c_array("ENC_URL", encrypted))
    print(f"\n# Verify decrypt: {verify_decrypt(encrypted, key)}")
    print()

    # Juga print rolling XOR (key "iq80" pattern dari .so)
    # "iq80" dipakai 3x berulang — artinya rolling XOR dengan key extension
    rolling_key = (key * 10)[:len(url)]
    rolling_enc = xor_encrypt(url, rolling_key)
    print(f"# Rolling variant (key repeated to match URL length):")
    print(to_c_array("ENC_URL_ROLLING", rolling_enc))
