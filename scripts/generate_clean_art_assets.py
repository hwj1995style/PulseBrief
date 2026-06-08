from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "mobile" / "assets" / "images"
SCALE = 3
W, H = 960, 560


def rgba(hex_color: str, alpha: int = 255) -> tuple[int, int, int, int]:
    hex_color = hex_color.lstrip("#")
    return (
        int(hex_color[0:2], 16),
        int(hex_color[2:4], 16),
        int(hex_color[4:6], 16),
        alpha,
    )


def sc(value: float) -> int:
    return int(round(value * SCALE))


def box(x0: float, y0: float, x1: float, y1: float) -> tuple[int, int, int, int]:
    return sc(x0), sc(y0), sc(x1), sc(y1)


def make_canvas() -> Image.Image:
    return Image.new("RGBA", (W * SCALE, H * SCALE), (0, 0, 0, 0))


def blur_layer(base: Image.Image, radius: float = 16) -> Image.Image:
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.ellipse(box(240, 126, 860, 520), fill=rgba("#7fb1ff", 30))
    return layer.filter(ImageFilter.GaussianBlur(sc(radius)))


def draw_soft_globe(draw: ImageDraw.ImageDraw, cx: float, cy: float, r: float) -> None:
    draw.ellipse(
        box(cx - r, cy - r, cx + r, cy + r),
        fill=rgba("#dbeaff", 140),
        outline=rgba("#87b8ff", 120),
        width=sc(2),
    )
    draw.ellipse(box(cx - r * 0.7, cy - r * 0.78, cx + r * 0.72, cy + r * 0.76), outline=rgba("#ffffff", 150), width=sc(2))
    draw.ellipse(box(cx - r * 0.36, cy - r * 0.88, cx + r * 0.38, cy + r * 0.88), outline=rgba("#ffffff", 125), width=sc(2))
    for offset in [-0.42, -0.18, 0.08, 0.34]:
        y = cy + r * offset
        draw.arc(box(cx - r * 0.88, y - r * 0.22, cx + r * 0.88, y + r * 0.22), 0, 360, fill=rgba("#ffffff", 120), width=sc(2))
    for angle in range(-50, 70, 24):
        rad = math.radians(angle)
        x0 = cx + math.sin(rad) * r * 0.18
        draw.arc(box(x0 - r * 0.52, cy - r * 0.94, x0 + r * 0.52, cy + r * 0.94), 70, 290, fill=rgba("#ffffff", 85), width=sc(1.5))

    # simplified continent dots and patches
    patches = [
        (cx - r * 0.28, cy - r * 0.18, 42, 30),
        (cx + r * 0.10, cy + r * 0.02, 54, 38),
        (cx + r * 0.35, cy - r * 0.30, 34, 24),
        (cx - r * 0.05, cy + r * 0.34, 36, 24),
    ]
    for x, y, pw, ph in patches:
        draw.rounded_rectangle(box(x - pw, y - ph, x + pw, y + ph), radius=sc(20), fill=rgba("#8abfff", 95))


def draw_orbits(draw: ImageDraw.ImageDraw, cx: float, cy: float) -> None:
    draw.arc(box(cx - 390, cy - 190, cx + 390, cy + 190), 190, 354, fill=rgba("#b9d6ff", 110), width=sc(3))
    draw.arc(box(cx - 410, cy - 215, cx + 410, cy + 215), 15, 165, fill=rgba("#ffffff", 135), width=sc(2))
    draw.arc(box(cx - 350, cy - 150, cx + 350, cy + 150), 205, 342, fill=rgba("#6fa9ff", 90), width=sc(2))


def draw_bars(draw: ImageDraw.ImageDraw, x: float, y: float, heights: list[int]) -> None:
    colors = ["#d9eaff", "#bcd8ff", "#86b8ff", "#5e9bff", "#2f74df"]
    for idx, h in enumerate(heights):
        x0 = x + idx * 44
        draw.rounded_rectangle(box(x0, y - h, x0 + 28, y), radius=sc(10), fill=rgba(colors[idx % len(colors)], 165))


def draw_arrow(draw: ImageDraw.ImageDraw, points: list[tuple[float, float]]) -> None:
    scaled = [(sc(x), sc(y)) for x, y in points]
    draw.line(scaled, fill=rgba("#2f74df", 190), width=sc(8), joint="curve")
    x, y = points[-1]
    draw.polygon([(sc(x), sc(y)), (sc(x - 34), sc(y + 8)), (sc(x - 10), sc(y + 34))], fill=rgba("#2f74df", 190))


def draw_coins(draw: ImageDraw.ImageDraw, x: float, y: float) -> None:
    for idx in range(4):
        yy = y - idx * 13
        draw.ellipse(box(x, yy, x + 92, yy + 30), fill=rgba("#b7d5ff", 160), outline=rgba("#ffffff", 150), width=sc(2))


def draw_chip(draw: ImageDraw.ImageDraw, x: float, y: float, label: str = "AI") -> None:
    draw.rounded_rectangle(box(x, y, x + 92, y + 92), radius=sc(26), fill=rgba("#2f74df", 190))
    draw.rounded_rectangle(box(x + 13, y + 13, x + 79, y + 79), radius=sc(14), outline=rgba("#ffffff", 160), width=sc(4))
    if label:
        try:
            font_size = sc(24)
            from PIL import ImageFont

            font = ImageFont.truetype("arial.ttf", font_size)
        except Exception:
            font = None
        draw.text((sc(x + 26), sc(y + 27)), label, fill=rgba("#ffffff", 235), font=font)


def save_asset(name: str, variant: str) -> None:
    img = make_canvas()
    img.alpha_composite(blur_layer(img))
    draw = ImageDraw.Draw(img)

    if variant == "finance":
        globe = (615, 245, 142)
        bars = [98, 150, 120, 190, 160]
        arrow = [(438, 345), (500, 285), (570, 304), (650, 210), (766, 122)]
    elif variant == "digest":
        globe = (620, 260, 150)
        bars = [70, 120, 96, 144, 112]
        arrow = [(440, 360), (510, 318), (590, 330), (675, 260), (780, 220)]
    elif variant == "player":
        globe = (620, 250, 160)
        bars = [64, 110, 82, 132, 96]
        arrow = [(420, 365), (500, 300), (588, 320), (690, 230), (810, 178)]
    elif variant == "subscription":
        globe = (650, 246, 140)
        bars = [82, 126, 108, 156, 128]
        arrow = [(440, 350), (516, 292), (590, 304), (664, 248), (770, 190)]
    else:
        globe = (620, 250, 148)
        bars = [84, 130, 104, 160, 124]
        arrow = [(430, 350), (500, 300), (580, 314), (668, 238), (788, 168)]

    draw_orbits(draw, globe[0], globe[1])
    draw_soft_globe(draw, globe[0], globe[1], globe[2])
    draw_bars(draw, 455, 434, bars)
    draw_arrow(draw, arrow)

    if variant == "finance":
        draw_coins(draw, 720, 396)
        draw.pieslice(box(410, 360, 535, 485), start=20, end=310, fill=rgba("#6ea8ff", 125))
        draw.pieslice(box(410, 360, 535, 485), start=310, end=20, fill=rgba("#ffffff", 145))
    elif variant == "digest":
        draw_chip(draw, 748, 360, "")
        draw.rounded_rectangle(box(760, 394, 824, 404), radius=sc(8), fill=rgba("#ffffff", 230))
        draw.rounded_rectangle(box(760, 418, 812, 428), radius=sc(8), fill=rgba("#ffffff", 210))
    elif variant == "player":
        for i, h in enumerate([38, 70, 100, 70, 38]):
            x = 150 + i * 36
            draw.rounded_rectangle(box(x, 270 - h / 2, x + 18, 270 + h / 2), radius=sc(9), fill=rgba("#0b55d9", 190))
    elif variant == "subscription":
        draw.ellipse(box(735, 350, 850, 465), fill=rgba("#6ea8ff", 150), outline=rgba("#ffffff", 160), width=sc(3))
        draw.line([(sc(765), sc(407)), (sc(794), sc(435)), (sc(832), sc(382))], fill=rgba("#ffffff", 240), width=sc(8))
    else:
        draw_chip(draw, 742, 350, "AI")

    # add tiny sparkles
    for x, y, a in [(360, 155, 105), (808, 292, 130), (520, 125, 90), (715, 110, 80)]:
        draw.line([(sc(x - 10), sc(y)), (sc(x + 10), sc(y))], fill=rgba("#ffffff", a), width=sc(2))
        draw.line([(sc(x), sc(y - 10)), (sc(x), sc(y + 10))], fill=rgba("#ffffff", a), width=sc(2))

    img = img.resize((W, H), Image.Resampling.LANCZOS)
    img.save(OUT_DIR / name)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    save_asset("art_clean_global.png", "global")
    save_asset("art_clean_finance.png", "finance")
    save_asset("art_clean_digest.png", "digest")
    save_asset("art_clean_player.png", "player")
    save_asset("art_clean_subscription.png", "subscription")
    print("Generated clean art assets in", OUT_DIR)


if __name__ == "__main__":
    main()
