#!/usr/bin/env python3
"""Draws the Kineflux icon and writes the Android mipmap tree into tool/android_res/. Needs Pillow."""
import pathlib
from PIL import Image, ImageDraw, ImageFilter

OUT = pathlib.Path(__file__).resolve().parent / 'android_res'
N = 1024
TEAL, VIOLET, PINK = (79, 213, 166), (139, 107, 224), (255, 55, 95)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(n, stops):
    img = Image.new('RGB', (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            t = min(1.0, max(0.0, ((x + y) / (2.0 * (n - 1)) - 0.22) / 0.56))
            if t < 0.5:
                px[x, y] = lerp(stops[0], stops[1], t * 2)
            else:
                px[x, y] = lerp(stops[1], stops[2], (t - 0.5) * 2)
    return img


def background(n):
    img = Image.new('RGB', (n, n))
    px = img.load()
    for y in range(n):
        c = lerp((28, 29, 40), (10, 10, 14), y / (n - 1))
        for x in range(n):
            px[x, y] = c
    glow = Image.new('RGB', (n, n), VIOLET)
    m = Image.new('L', (n, n), 0)
    ImageDraw.Draw(m).ellipse((n * .25, n * .25, n * .85, n * .85), fill=70)
    m = m.filter(ImageFilter.GaussianBlur(n * .12))
    img.paste(glow, (0, 0), m)
    return img


def mark(n):
    """Keyframe diamond with a play triangle and motion trails, on a transparent layer."""
    k = 2  # supersample
    s = n * k
    layer = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    cx, cy = .57 * s, .5 * s
    diag = .46 * s
    side = diag / 2 ** .5
    # diamond mask: rounded square rotated 45 degrees
    sq = Image.new('L', (int(side * 1.6), int(side * 1.6)), 0)
    o = (sq.width - side) / 2
    ImageDraw.Draw(sq).rounded_rectangle((o, o, o + side, o + side), radius=side * .2, fill=255)
    sq = sq.rotate(45, resample=Image.BICUBIC, expand=True)
    grad = gradient(sq.width, [TEAL, VIOLET, PINK])
    pos = (int(cx - sq.width / 2), int(cy - sq.height / 2))
    layer.paste(grad, pos, sq)
    d = ImageDraw.Draw(layer)
    # play triangle
    tri = [(cx - .055 * s, cy - .085 * s), (cx - .055 * s, cy + .085 * s), (cx + .095 * s, cy)]
    d.polygon(tri, fill=(255, 255, 255, 255))
    # motion trails
    left = cx - diag / 2 - .03 * s
    for i, (dy, ln, a) in enumerate([(-.085, .12, 230), (0, .15, 255), (.085, .09, 190)]):
        y = cy + dy * s
        h = .026 * s
        d.rounded_rectangle((left - ln * s, y - h, left, y + h), radius=h, fill=(255, 255, 255, a))
    return layer.resize((n, n), Image.LANCZOS)


def rounded(img, r):
    m = Image.new('L', img.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, img.width - 1, img.height - 1), radius=int(img.width * r), fill=255)
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), m)
    return out


bg = background(N)
fg = mark(N)
legacy = bg.convert('RGBA')
legacy.alpha_composite(fg)
legacy = rounded(legacy, .22)

LEGACY = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
ADAPT = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}
for d, px in LEGACY.items():
    p = OUT / f'mipmap-{d}'
    p.mkdir(parents=True, exist_ok=True)
    legacy.resize((px, px), Image.LANCZOS).save(p / 'ic_launcher.png')
    a = ADAPT[d]
    bg.resize((a, a), Image.LANCZOS).save(p / 'ic_launcher_background.png')
    fg.resize((a, a), Image.LANCZOS).save(p / 'ic_launcher_foreground.png')
v26 = OUT / 'mipmap-anydpi-v26'
v26.mkdir(parents=True, exist_ok=True)
(v26 / 'ic_launcher.xml').write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
    '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
    '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
    '</adaptive-icon>\n')
legacy.save(OUT.parent / 'icon_preview.png')
print('icons written to', OUT)
