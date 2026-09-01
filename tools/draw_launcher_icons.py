"""Draws the launcher icons for phones too old for adaptive icons.

Android 8 and later build the icon from the vector in res/drawable, but Android 6 and 7 need
a finished picture at each screen density. This draws the same steering wheel so the app looks
the same wherever it is installed, rather than falling back to the icon it was forked from.

Usage: python tools/draw_launcher_icons.py
"""

import os
from PIL import Image, ImageDraw

BACKGROUND = (36, 28, 46, 255)   # #241C2E
MARK = (240, 233, 247, 255)      # #F0E9F7

# density suffix and the icon size Android expects for it
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# drawn on a 108 unit canvas, matching the vector, then scaled to each size
CANVAS = 108.0
SUPERSAMPLE = 8


def draw_wheel(size):
    """Draws one icon, oversampled and then shrunk so the curves come out smooth."""
    big = int(size * SUPERSAMPLE)
    image = Image.new("RGBA", (big, big), BACKGROUND)
    draw = ImageDraw.Draw(image)
    unit = big / CANVAS

    centre = 54 * unit
    rim_radius = 30 * unit
    stroke = 9 * unit

    draw.ellipse(
        [centre - rim_radius, centre - rim_radius, centre + rim_radius, centre + rim_radius],
        outline=MARK,
        width=int(stroke),
    )

    for end in ((54, 26), (29, 69), (79, 69)):
        draw.line(
            [centre, centre, end[0] * unit, end[1] * unit],
            fill=MARK,
            width=int(stroke),
        )
        # round off the spoke ends, as the vector does
        cap = stroke / 2
        draw.ellipse(
            [end[0] * unit - cap, end[1] * unit - cap,
             end[0] * unit + cap, end[1] * unit + cap],
            fill=MARK,
        )

    hub = 11 * unit
    draw.ellipse([centre - hub, centre - hub, centre + hub, centre + hub], fill=MARK)

    return image.resize((size, size), Image.LANCZOS)


def round_off(image):
    """The round variant, for launchers that ask for one."""
    size = image.size[0]
    mask = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size * 4, size * 4], fill=255)
    rounded = image.copy()
    rounded.putalpha(mask.resize((size, size), Image.LANCZOS))
    return rounded


def main():
    root = os.path.join("app", "src", "main", "res")
    for density, size in DENSITIES.items():
        folder = os.path.join(root, "mipmap-" + density)
        os.makedirs(folder, exist_ok=True)

        icon = draw_wheel(size)
        icon.save(os.path.join(folder, "ic_launcher.png"))
        round_off(icon).save(os.path.join(folder, "ic_launcher_round.png"))
        print("wrote %s at %dpx" % (folder, size))


if __name__ == "__main__":
    main()
