#!/usr/bin/env python3
"""Gera os icones do DS5 Printer Service a partir do simbolo DS5.

Origem:
    branding/ds5_symbol.png   simbolo quadrado, transparente (>= 512px), o mesmo do DS5 Connect

Gera:
    mipmap-*/ic_launcher_foreground.png   camada da frente do icone adaptativo (simbolo + selo de impressora)
    mipmap-*/ic_launcher_monochrome.png   icone tematico do Android 13+ (so o alfa conta)
    mipmap-*/ic_launcher.png              icone legado (Android < 8), fundo branco quadrado arredondado
    mipmap-*/ic_launcher_round.png        icone legado redondo
    drawable-nodpi/ds5_symbol.png         simbolo para o cabecalho do app
    branding/ic_launcher_preview.png      previa 512px para conferencia

Rode sempre que a arte mudar:

    python tools/ds5_branding.py
"""

import pathlib
import sys

from PIL import Image, ImageDraw

REPO = pathlib.Path(__file__).resolve().parent.parent
RES = REPO / "app" / "src" / "main" / "res"
SYMBOL = REPO / "branding" / "ds5_symbol.png"

INK = (5, 7, 8, 255)  # preto do simbolo DS5 (#050708)
WHITE = (255, 255, 255, 255)

# Densidades Android: fator sobre mdpi.
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}

# Geometria do icone adaptativo, em dp, sobre a tela de 108dp.
# A zona segura e um circulo de raio 33dp no centro (54, 54).
SS = 8  # supersampling: desenha a 8x e reduz, para bordas limpas
SYMBOL_BOX = (27, 27, 75, 75)  # simbolo com 48dp
BADGE_CENTER = (68.5, 68.5)
BADGE_RADIUS = 12.5
BADGE_RING = 2.5  # aro branco que separa o selo do simbolo


def load_symbol() -> Image.Image:
    if not SYMBOL.is_file():
        sys.exit(f"ERRO: arte ausente: {SYMBOL}")
    img = Image.open(SYMBOL).convert("RGBA")
    img = img.crop(img.getbbox())
    side = max(img.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
    return canvas


def draw_printer(draw: ImageDraw.ImageDraw, cx: float, cy: float, s: float, fg, bg) -> None:
    """Desenha um icone de impressora (estilo Material "print") centrado em (cx, cy).

    s e o lado da caixa de 24 unidades do icone, em pixels.
    """
    u = s / 24.0

    def box(x0, y0, x1, y1):
        return (cx + (x0 - 12) * u, cy + (y0 - 12) * u, cx + (x1 - 12) * u, cy + (y1 - 12) * u)

    draw.rectangle(box(6, 3, 18, 7), fill=fg)  # papel em espera
    draw.rounded_rectangle(box(2, 8, 22, 17), radius=3 * u, fill=fg)  # corpo
    draw.rectangle(box(6, 13, 18, 21), fill=fg)  # bandeja de saida
    draw.rectangle(box(8, 14, 16, 19), fill=bg)  # folha impressa (vazada)
    draw.ellipse(box(18, 10, 20, 12), fill=bg)  # led


def foreground(symbol: Image.Image, px: int, with_badge_bg: bool = True) -> Image.Image:
    """Camada da frente em px x px (equivale a 108dp)."""
    big = px * SS
    k = big / 108.0
    layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    x0, y0, x1, y1 = (round(v * k) for v in SYMBOL_BOX)
    layer.alpha_composite(symbol.resize((x1 - x0, y1 - y0), Image.LANCZOS), (x0, y0))

    draw = ImageDraw.Draw(layer)
    cx, cy = (v * k for v in BADGE_CENTER)
    r = BADGE_RADIUS * k
    ring = (BADGE_RADIUS + BADGE_RING) * k
    if with_badge_bg:
        draw.ellipse((cx - ring, cy - ring, cx + ring, cy + ring), fill=WHITE)
    else:
        # monocromatico: abre o aro como transparencia
        hole = Image.new("L", (big, big), 0)
        ImageDraw.Draw(hole).ellipse((cx - ring, cy - ring, cx + ring, cy + ring), fill=255)
        alpha = layer.getchannel("A")
        alpha.paste(0, mask=hole)
        layer.putalpha(alpha)
        draw = ImageDraw.Draw(layer)
    draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=INK)
    draw_printer(draw, cx, cy, r * 1.25, WHITE, INK)
    return layer.resize((px, px), Image.LANCZOS)


def monochrome(symbol: Image.Image, px: int) -> Image.Image:
    """O sistema recolore: so o alfa importa. O selo vira um disco com a impressora vazada."""
    fg = foreground(symbol, px * 2, with_badge_bg=False)
    # Impressora vazada: pinta de transparente onde o glifo e branco.
    r, g, b, a = fg.split()
    lum = Image.merge("RGB", (r, g, b)).convert("L")
    glyph = lum.point(lambda v: 255 if v > 200 else 0)
    a.paste(0, mask=glyph)
    out = Image.new("RGBA", fg.size, (0, 0, 0, 0))
    black = Image.new("RGBA", fg.size, (0, 0, 0, 255))
    out.paste(black, mask=a)
    return out.resize((px, px), Image.LANCZOS)


def legacy(symbol: Image.Image, px: int, round_shape: bool) -> Image.Image:
    """Icone legado de 48dp: fundo branco + a mesma composicao do adaptativo."""
    big = px * SS
    out = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(out)
    pad = big * 2 / 48  # 2dp de margem, como os icones do sistema
    if round_shape:
        draw.ellipse((pad, pad, big - pad, big - pad), fill=WHITE)
    else:
        draw.rounded_rectangle((pad, pad, big - pad, big - pad), radius=big * 0.18, fill=WHITE)
    # A tela de 108dp do adaptativo mostra 72dp; aqui mostramos o mesmo recorte em 44dp.
    fg = foreground(symbol, round(big * 108 / 72 * 44 / 48))
    off = (big - fg.width) // 2
    out.alpha_composite(fg, (off, off))
    return out.resize((px, px), Image.LANCZOS)


def recolor_ink(img: Image.Image, rgb: tuple) -> Image.Image:
    """Troca os pixels escuros (o preto do simbolo) por rgb, mantendo o verde e o alfa."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a and max(r, g, b) < 90:
                px[x, y] = rgb + (a,)
    return out


def save(img: Image.Image, path: pathlib.Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG", optimize=True)
    print(f"  {path.relative_to(REPO)}  {img.width}x{img.height}")


def main() -> None:
    symbol = load_symbol()
    for name, f in DENSITIES.items():
        folder = RES / f"mipmap-{name}"
        save(foreground(symbol, round(108 * f)), folder / "ic_launcher_foreground.png")
        save(monochrome(symbol, round(108 * f)), folder / "ic_launcher_monochrome.png")
        save(legacy(symbol, round(48 * f), round_shape=False), folder / "ic_launcher.png")
        save(legacy(symbol, round(48 * f), round_shape=True), folder / "ic_launcher_round.png")

    small = symbol.resize((256, 256), Image.LANCZOS)
    # Original colorido: pagina de teste impressa (nunca muda com o tema).
    save(small, RES / "drawable-nodpi" / "ds5_symbol.png")
    # Cabecalho do app: no modo escuro o preto do simbolo some, entao vira quase branco.
    save(small, RES / "drawable-nodpi" / "ds5_symbol_header.png")
    save(recolor_ink(small, (236, 236, 236)), RES / "drawable-night-nodpi" / "ds5_symbol_header.png")

    # Previa: como o launcher mostra (mascara circular sobre fundo branco).
    prev = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    mask = Image.new("L", (512, 512), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, 511, 511), fill=255)
    tile = Image.new("RGBA", (768, 768), WHITE)
    tile.alpha_composite(foreground(symbol, 768))
    prev.paste(tile.crop((128, 128, 640, 640)), (0, 0), mask)
    save(prev, REPO / "branding" / "ic_launcher_preview.png")


if __name__ == "__main__":
    main()
