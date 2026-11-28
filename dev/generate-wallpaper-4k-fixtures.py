#!/usr/bin/env python3
"""Deterministic 3840x2160 wallpaper fixtures for the 4K wallpaper probe (debug fixture provider only).

Each picture carries two detail patches that only survive a full-resolution path:
  * rows 960-1199, every column: one-pixel vertical black/white stripes (horizontal detail);
  * columns 1600-2239, rows 1320-1559: one-pixel horizontal stripes (vertical detail).
A 1920x1080 decode or a scaled presentation turns both patches into flat grey, which is what
WallpaperResolutionProbeActivity measures. Needs Pillow and ffmpeg with libx265 and libwebp.

Usage: dev/generate-wallpaper-4k-fixtures.py        (writes into launcher/wallpaper-fixture/src/main/res/raw)
"""
import shutil, subprocess, sys, tempfile
from pathlib import Path

if len(sys.argv) > 1 and sys.argv[1] in ('-h', '--help'):
    print(__doc__); sys.exit(0)

from PIL import Image, ImageDraw

W, H = 3840, 2160
RAW = Path(__file__).resolve().parents[1] / 'launcher/wallpaper-fixture/src/main/res/raw'
STRIPE_ROWS = (960, 1200)
BLOCK = (1600, 1320, 2240, 1560)


def picture(top, bottom, square_x=None):
    image = Image.new('RGB', (W, H))
    draw = ImageDraw.Draw(image)
    for y in range(H):  # vertical gradient: one colour per row, compresses to almost nothing
        t = y / (H - 1)
        draw.line([(0, y), (W - 1, y)], fill=tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    stripes = Image.new('L', (W, 1))
    stripes.putdata([255 if x % 2 else 0 for x in range(W)])
    band = stripes.resize((W, STRIPE_ROWS[1] - STRIPE_ROWS[0]), Image.NEAREST).convert('RGB')
    image.paste(band, (0, STRIPE_ROWS[0]))
    x0, y0, x1, y1 = BLOCK
    for y in range(y0, y1):
        draw.line([(x0, y), (x1 - 1, y)], fill=(255, 255, 255) if y % 2 else (0, 0, 0))
    if square_x is not None:  # motion marker, away from both detail patches
        draw.rectangle([square_x, 300, square_x + 239, 539], fill=(250, 200, 40))
    return image


def main():
    for tool in ('ffmpeg',):
        if not shutil.which(tool):
            sys.exit(f'{tool} is required')
    RAW.mkdir(parents=True, exist_ok=True)
    picture((18, 38, 72), (12, 92, 104)).save(RAW / 'uhd_stripes.png', optimize=True)
    picture((72, 30, 52), (110, 64, 20)).save(RAW / 'uhd_stripes_b.png', optimize=True)
    # Every frame changes the whole canvas (the gradient shifts), the worst case for a frame-by-frame decoder.
    frames = [picture((18 + 6 * i, 38, 72), (12, 92 - 4 * i, 104), 200 + i * 280) for i in range(12)]
    frames[0].save(RAW / 'uhd_animated.webp', save_all=True, append_images=frames[1:], duration=100,
                   loop=0, lossless=True, method=4)
    # The same animation at smaller source sizes, for the animated-source matrix
    # (docs/plans/2026-11-28-animated-wallpapers-from-large-sources.md); the stripes do not survive, nor need to.
    for name, size in (('animated_1440', (2560, 1440)), ('animated_1080', (1920, 1080)), ('animated_720', (1280, 720))):
        small = [frame.resize(size, Image.LANCZOS) for frame in frames]
        small[0].save(RAW / f'{name}.webp', save_all=True, append_images=small[1:], duration=100,
                      loop=0, lossless=True, method=4)
    with tempfile.TemporaryDirectory() as tmp:
        for i in range(30):
            picture((18, 38, 72), (12, 92, 104), 200 + i * 110).save(f'{tmp}/f{i:03d}.png')
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-framerate', '30', '-i', f'{tmp}/f%03d.png',
                        '-c:v', 'libx265', '-preset', 'medium', '-x265-params', 'log-level=error:qp=4',
                        '-pix_fmt', 'yuv420p', '-tag:v', 'hvc1', '-an', str(RAW / 'uhd_video.mp4')], check=True)
    for name in ('uhd_stripes.png', 'uhd_stripes_b.png', 'uhd_animated.webp', 'animated_1440.webp',
                 'animated_1080.webp', 'animated_720.webp', 'uhd_video.mp4'):
        print(f'{name}: {(RAW / name).stat().st_size:,} bytes')


if __name__ == '__main__':
    main()
